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
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Applies existing Liquibase changelogs to an empty DB (H2 by default), diffs against current
 * {@code @EliasTable} entities, and writes one formatted-sql changeset when there are changes.
 */
@Mojo(name = "changelog-export", defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class ChangelogExportMojo extends AbstractMojo {

    private static final Pattern CHANGE_FILE = Pattern.compile("^(\\d+).*$");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

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

    @Parameter(property = "jdbcUrl",
            defaultValue = "jdbc:h2:mem:elias_export;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DEFAULT_NULL_ORDERING=HIGH")
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
        try {
            Path changeLogDirPath = changeLogDir.toPath().toAbsolutePath().normalize();
            if (!Files.isDirectory(changeLogDirPath)) {
                throw new MojoExecutionException("changeLogDir does not exist: " + changeLogDirPath);
            }

            ClassLoader projectClassLoader = buildProjectClassLoader();
            List<TableSpec> tableSpecs = loadTableSpecs(projectClassLoader);
            getLog().info("Scanned " + tableSpecs.size() + " Elias tables from " + scanPackages);

            try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
                runLiquibaseUpdate(connection, changeLogDirPath);

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
                if (diff.isEmpty()) {
                    getLog().info("No schema changes");
                    return;
                }

                Path output = resolveOutput(changeLogDirPath);
                String id = changesetId != null && !changesetId.isBlank()
                        ? changesetId
                        : output.getFileName().toString().replaceFirst("\\.sql$", "");
                LiquibaseSqlExporter exporter = new LiquibaseSqlExporter();
                exporter.export(diff, output, id, author);
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

    private void runLiquibaseUpdate(Connection connection, Path changeLogDirPath) throws Exception {
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
        try (Liquibase liquibase = new Liquibase(
                master.getFileName().toString(),
                new DirectoryResourceAccessor(master.getParent().toFile()),
                database)) {
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception ex) {
            if (isH2JdbcUrl(jdbcUrl)) {
                throw new MojoExecutionException(
                        "Liquibase update failed on H2. Exported DDL targets MySQL; H2 MODE=MySQL "
                                + "cannot replay every statement (e.g. SPATIAL INDEX, CHANGE COLUMN). "
                                + "Re-run with a MySQL scratch database, e.g. "
                                + "-DjdbcUrl=jdbc:mysql://127.0.0.1:3306/elias_export "
                                + "-DjdbcUser=... -DjdbcPassword=... "
                                + "Root cause: " + ex.getMessage(),
                        ex);
            }
            throw ex;
        }
    }

    private static boolean isH2JdbcUrl(String url) {
        return url != null && url.toLowerCase().startsWith("jdbc:h2:");
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
        List<URL> urls = new ArrayList<>();
        for (String path : project.getCompileClasspathElements()) {
            urls.add(new File(path).toURI().toURL());
        }
        File output = new File(project.getBuild().getOutputDirectory());
        if (output.exists()) {
            urls.add(output.toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
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
