package cc.ddrpa.dorian.elias.maven;

import cc.ddrpa.dorian.elias.core.EntitySearcher;
import cc.ddrpa.dorian.elias.core.SpecMaker;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.validation.SchemaDefinitionIssue;
import cc.ddrpa.dorian.elias.core.validation.SchemaDefinitionValidator;
import cc.ddrpa.dorian.elias.generator.export.DiffResult;
import cc.ddrpa.dorian.elias.generator.export.ExportChange;
import cc.ddrpa.dorian.elias.generator.export.ExportDiffer;
import cc.ddrpa.dorian.elias.generator.export.LiquibaseSqlExporter;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies existing Liquibase changelogs to an empty DB (H2 by default), diffs against current
 * {@code @EliasTable} entities, and writes one formatted-sql changeset when there are changes.
 * <p>
 * Compile classpath is reactor-aware: sibling modules use {@code target/classes} so they need not
 * be installed to the local repository. Invocation stays {@code elias:changelog-export}.
 */
@Mojo(name = "changelog-export", defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.COMPILE)
public class ChangelogExportMojo extends AbstractMojo {

    private static final Pattern CHANGE_FILE = Pattern.compile("^(\\d+).*$");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepositories;

    @Inject
    private RepositorySystem repositorySystem;

    /**
     * Packages to scan for {@code @EliasTable}.
     */
    @Parameter(property = "scanPackages", required = true)
    private List<String> scanPackages;

    /**
     * Directory containing Liquibase change files (numbered).
     */
    @Parameter(property = "changeLogDir", required = true)
    private File changeLogDir;

    /**
     * Master changelog relative to {@link #changeLogDir}, or absolute path.
     */
    @Parameter(property = "changeLogMaster", defaultValue = "db.changelog-master.yaml")
    private String changeLogMaster;

    /**
     * Scratch JDBC URL. Default is an in-memory H2 database in MySQL mode.
     * <p>
     * Do not use {@code DB_CLOSE_DELAY=-1} with a fixed {@code mem:} name: a previous run in the
     * same JVM can leave {@code databasechangelog} behind and Liquibase will fail recreating it
     * under {@code DATABASE_TO_LOWER=TRUE}. Each run also rewrites the mem name to a unique value.
     */
    @Parameter(property = "jdbcUrl",
            defaultValue = "jdbc:h2:mem:elias_export;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DEFAULT_NULL_ORDERING=HIGH")
    private String jdbcUrl;

    @Parameter(property = "jdbcUser", defaultValue = "sa")
    private String jdbcUser;

    @Parameter(property = "jdbcPassword", defaultValue = "")
    private String jdbcPassword;

    @Parameter(property = "author", defaultValue = "elias")
    private String author;

    /**
     * Optional explicit output file. When empty, next numbered file under changeLogDir/changes is used.
     */
    @Parameter(property = "out")
    private File out;

    @Parameter(property = "changesetId")
    private String changesetId;

    /**
     * Comma-separated or repeated rename mappings: {@code old:new} or {@code table.old:new}.
     */
    @Parameter(property = "rename")
    private List<String> rename;

    /**
     * When true, omit {@code DROP COLUMN} from the exported changeset.
     * Default {@code false}: destructive column drops are included.
     */
    @Parameter(property = "excludeDropColumn", defaultValue = "false")
    private boolean excludeDropColumn;

    /**
     * When true, omit extra {@code DROP INDEX} (indexes present in DB but not on entities).
     * Recreate-on-mismatch still emits DROP + CREATE. Default {@code false}.
     */
    @Parameter(property = "excludeDropIndex", defaultValue = "false")
    private boolean excludeDropIndex;

    @Override
    public void execute() throws MojoExecutionException {
        if ("pom".equalsIgnoreCase(project.getPackaging())) {
            getLog().info("Skipping changelog-export on packaging=pom project "
                    + project.getArtifactId());
            return;
        }
        try {
            Path changeLogDirPath = changeLogDir.toPath().toAbsolutePath().normalize();
            if (!Files.isDirectory(changeLogDirPath)) {
                throw new MojoExecutionException("changeLogDir does not exist: " + changeLogDirPath);
            }

            ClassLoader projectClassLoader = buildProjectClassLoader();
            List<TableSpec> tableSpecs = loadTableSpecs(projectClassLoader);
            getLog().info("Scanned " + tableSpecs.size() + " Elias tables from " + scanPackages);

            String effectiveJdbcUrl = uniqueH2MemUrl(jdbcUrl);
            try (Connection connection = DriverManager.getConnection(
                    effectiveJdbcUrl, jdbcUser, jdbcPassword)) {
                runLiquibaseUpdate(connection, changeLogDirPath, effectiveJdbcUrl);

                ExportDiffer differ = new ExportDiffer(connection,
                        new cc.ddrpa.dorian.elias.generator.MySQL57Generator().setDropIfExists(false));
                if (rename != null) {
                    for (String mapping : rename) {
                        if (mapping != null && !mapping.isBlank()) {
                            for (String part : mapping.split(",")) {
                                differ.rename(part.trim());
                            }
                        }
                    }
                }
                DiffResult diff = applyDropExclusions(differ.diff(tableSpecs));
                if (!diff.hasExportableSql()) {
                    getLog().info("No schema changes");
                    return;
                }

                Path output = resolveOutput(changeLogDirPath);
                String id = changesetId != null && !changesetId.isBlank()
                        ? changesetId
                        : output.getFileName().toString().replaceFirst("\\.sql$", "");
                LiquibaseSqlExporter exporter = new LiquibaseSqlExporter();
                if (!exporter.export(diff, output, id, author)) {
                    getLog().info("No schema changes");
                    return;
                }
                getLog().info("Wrote changeset: " + output.toAbsolutePath());
                if (!diff.getDestructiveChanges().isEmpty()) {
                    getLog().warn("Destructive changes included:\n"
                            + exporter.summarizeDestructive(diff));
                }
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("changelog-export failed", e);
        }
    }

    private DiffResult applyDropExclusions(DiffResult diff) {
        DiffResult filtered = diff;
        if (excludeDropColumn) {
            getLog().info("excludeDropColumn=true: DROP COLUMN omitted from export");
            filtered = filtered.withoutKinds(EnumSet.of(ExportChange.Kind.DROP_COLUMN));
        }
        if (!excludeDropIndex) {
            return filtered;
        }
        getLog().info("excludeDropIndex=true: extra DROP INDEX omitted from export");
        DiffResult withoutExtraIndexes = new DiffResult();
        for (ExportChange change : filtered.getChanges()) {
            if (change.getKind() == ExportChange.Kind.DROP_INDEX
                    && change.getSummary().startsWith("drop extra index")) {
                continue;
            }
            withoutExtraIndexes.add(change);
        }
        return withoutExtraIndexes;
    }

    private void runLiquibaseUpdate(Connection connection, Path changeLogDirPath, String url)
            throws Exception {
        Path master = Path.of(changeLogMaster);
        if (!master.isAbsolute()) {
            master = changeLogDirPath.resolve(changeLogMaster);
        }
        if (!Files.exists(master)) {
            getLog().warn("Master changelog not found (" + master
                    + "); skipping liquibase update (empty baseline)");
            return;
        }
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        alignH2WithDatabaseToLower(database, url);
        // Do not close Liquibase/Database: Liquibase.close() -> Database.close() closes the
        // underlying JDBC Connection (try-with-resources on DatabaseConnection), but the caller
        // still needs it for ExportDiffer.
        Liquibase liquibase = new Liquibase(
                master.getFileName().toString(),
                new DirectoryResourceAccessor(master.getParent().toFile()),
                database);
        try {
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception ex) {
            if (isH2JdbcUrl(url)) {
                throw new MojoExecutionException(h2UpdateFailureMessage(ex), ex);
            }
            throw ex;
        }
    }

    /**
     * H2 {@code DATABASE_TO_LOWER=TRUE} stores identifiers in lowercase, but Liquibase's
     * {@link H2Database} still uppercases unquoted names, so it creates {@code databasechangelog}
     * then fails recreating {@code DATABASECHANGELOG}. Force lowercase unquoted mode to match.
     */
    private void alignH2WithDatabaseToLower(Database database, String url) {
        if (!(database instanceof H2Database) || !hasDatabaseToLower(url)) {
            return;
        }
        try {
            Field field = findField(database.getClass(), "unquotedObjectsAreUppercased");
            field.setAccessible(true);
            field.set(database, Boolean.FALSE);
            getLog().debug("Aligned Liquibase H2 unquotedObjectsAreUppercased=false for DATABASE_TO_LOWER");
        } catch (ReflectiveOperationException e) {
            getLog().warn("Could not align Liquibase H2 casing with DATABASE_TO_LOWER: "
                    + e.getMessage());
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Default mem name must not be reused across plugin invocations in the same JVM
     * (especially if a caller still sets {@code DB_CLOSE_DELAY=-1}).
     */
    static String uniqueH2MemUrl(String url) {
        if (!isH2JdbcUrl(url)) {
            return url;
        }
        // jdbc:h2:mem:name;params...
        int mem = url.indexOf(":mem:");
        if (mem < 0) {
            return url;
        }
        int nameStart = mem + 5;
        int nameEnd = url.indexOf(';', nameStart);
        if (nameEnd < 0) {
            nameEnd = url.length();
        }
        String name = url.substring(nameStart, nameEnd);
        if (name.isEmpty()) {
            return url;
        }
        String unique = name + "_" + System.nanoTime();
        return url.substring(0, nameStart) + unique + url.substring(nameEnd);
    }

    private static String h2UpdateFailureMessage(Exception ex) {
        String detail = rootMessage(ex);
        if (detail != null && detail.toLowerCase(Locale.ROOT).contains("databasechangelog")
                && detail.toLowerCase(Locale.ROOT).contains("already exists")) {
            return "Liquibase update failed on H2: DATABASECHANGELOG casing conflict "
                    + "(often DATABASE_TO_LOWER + Liquibase uppercase identifiers). "
                    + "Root cause: " + detail;
        }
        return "Liquibase update failed on H2. Exported DDL targets MySQL; H2 MODE=MySQL "
                + "cannot replay every statement (e.g. SPATIAL INDEX, CHANGE COLUMN). "
                + "Re-run with a MySQL scratch database, e.g. "
                + "-DjdbcUrl=jdbc:mysql://127.0.0.1:3306/elias_export "
                + "-DjdbcUser=... -DjdbcPassword=... "
                + "Root cause: " + detail;
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : ex.getMessage();
    }

    private static boolean isH2JdbcUrl(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:");
    }

    private static boolean hasDatabaseToLower(String url) {
        return url != null && url.toUpperCase(Locale.ROOT).contains("DATABASE_TO_LOWER=TRUE");
    }

    private List<TableSpec> loadTableSpecs(ClassLoader classLoader) throws MojoExecutionException {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            EntitySearcher searcher = new EntitySearcher().addPackages(scanPackages);
            List<TableSpec> tableSpecs = searcher.search().stream()
                    .map(SpecMaker::makeTableSpec)
                    .collect(Collectors.toList());
            validateDefinitions(tableSpecs);
            return tableSpecs;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private void validateDefinitions(List<TableSpec> tableSpecs) throws MojoExecutionException {
        SchemaDefinitionValidator validator = new SchemaDefinitionValidator();
        List<String> errors = new ArrayList<>();
        for (TableSpec tableSpec : tableSpecs) {
            for (SchemaDefinitionIssue issue : validator.validate(tableSpec)) {
                errors.add(tableSpec.getName() + ": " + issue.getType() + " "
                        + issue.getSubject() + " - " + issue.getDetail());
            }
        }
        if (!errors.isEmpty()) {
            throw new MojoExecutionException(
                    "Invalid schema definition, refusing changelog-export:\n"
                            + String.join("\n", errors));
        }
    }

    private ClassLoader buildProjectClassLoader() throws Exception {
        List<File> classpath = ReactorCompileClasspath.build(
                project, reactorProjects, this::resolveExternalArtifact);
        List<URL> urls = new ArrayList<>(classpath.size());
        for (File entry : classpath) {
            urls.add(entry.toURI().toURL());
            getLog().debug("changelog-export classpath: " + entry.getAbsolutePath());
        }
        return new URLClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
    }

    private File resolveExternalArtifact(Artifact artifact) throws Exception {
        if (artifact.getFile() != null && artifact.getFile().exists()) {
            return artifact.getFile();
        }
        String extension = artifact.getArtifactHandler() != null
                ? artifact.getArtifactHandler().getExtension()
                : artifact.getType();
        if (extension == null || extension.isBlank()) {
            extension = "jar";
        }
        String classifier = artifact.getClassifier();
        org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                classifier == null ? "" : classifier,
                extension,
                artifact.getVersion());
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(aetherArtifact);
        request.setRepositories(remoteRepositories);
        ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
        return result.getArtifact().getFile();
    }

    private Path resolveOutput(Path changeLogDirPath) throws Exception {
        if (out != null) {
            return out.toPath().toAbsolutePath().normalize();
        }
        Path changesDir = changeLogDirPath.resolve("changes");
        Files.createDirectories(changesDir);
        int next = nextChangeNumber(changesDir);
        String name = String.format("%04d-elias-export.sql", next);
        return changesDir.resolve(name);
    }

    private int nextChangeNumber(Path changesDir) throws Exception {
        int max = 0;
        if (!Files.isDirectory(changesDir)) {
            return 1;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(changesDir)) {
            for (Path path : stream) {
                Matcher matcher = CHANGE_FILE.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    max = Math.max(max, Integer.parseInt(matcher.group(1)));
                }
            }
        }
        return max + 1;
    }
}
