package cc.ddrpa.dorian.elias.generator.export;

import cc.ddrpa.dorian.elias.core.SpecMaker;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.Index;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.generator.MySQL57Generator;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportDifferTest {

    private static final Pattern ROLLBACK = Pattern.compile("^--rollback (.+)$", Pattern.MULTILINE);

    @Test
    void exportsAddAndDropAgainstLiquibaseStyleBaseline() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:elias_diff;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "")) {
            try (Statement st = connection.createStatement()) {
                st.execute("create table demo (id bigint not null, name varchar(50), obsolete varchar(10))");
            }

            TableSpec expected = new TableSpec().setName("demo");
            expected.getColumns().add(new ColumnSpec()
                    .setName("id")
                    .setDataType("bigint")
                    .setColumnType("bigint")
                    .setNullable(false)
                    .setPrimaryKey(true));
            expected.getColumns().add(new ColumnSpec()
                    .setName("name")
                    .setDataType("varchar")
                    .setColumnType("varchar(100)")
                    .setLength(100L)
                    .setNullable(true));
            expected.getColumns().add(new ColumnSpec()
                    .setName("extra")
                    .setDataType("varchar")
                    .setColumnType("varchar(20)")
                    .setLength(20L)
                    .setNullable(true));

            ExportDiffer differ = new ExportDiffer(
                    connection,
                    new MySQL57Generator().setDropIfExists(false));
            DiffResult diff = differ.diff(List.of(expected));
            assertFalse(diff.isEmpty());
            assertTrue(diff.getChanges().stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.DROP_COLUMN));
            assertTrue(diff.getChanges().stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.ADD_COLUMN));
            assertTrue(diff.getChanges().stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.MODIFY_COLUMN));

            for (ExportChange change : diff.getChanges()) {
                assertTrue(change.getRollbackSql() != null && !change.getRollbackSql().isBlank(),
                        () -> "missing rollback for " + change.getSummary());
            }

            ExportChange add = diff.getChanges().stream()
                    .filter(c -> c.getKind() == ExportChange.Kind.ADD_COLUMN)
                    .findFirst()
                    .orElseThrow();
            assertTrue(add.getSql().toLowerCase().contains("add column"));
            assertTrue(add.getRollbackSql().toLowerCase().contains("drop column"));

            ExportChange drop = diff.getChanges().stream()
                    .filter(c -> c.getKind() == ExportChange.Kind.DROP_COLUMN)
                    .findFirst()
                    .orElseThrow();
            assertTrue(drop.getSql().toLowerCase().contains("drop column"));
            assertTrue(drop.getRollbackSql().toLowerCase().contains("add column"));

            ExportChange modify = diff.getChanges().stream()
                    .filter(c -> c.getKind() == ExportChange.Kind.MODIFY_COLUMN)
                    .findFirst()
                    .orElseThrow();
            assertTrue(modify.getSql().toLowerCase().contains("modify column"));
            assertTrue(modify.getRollbackSql().toLowerCase().contains("modify column"));

            LiquibaseSqlExporter exporter = new LiquibaseSqlExporter();
            String rendered = exporter.render(diff, "0001-test", "elias");
            assertTrue(rendered.startsWith("--liquibase formatted sql"));
            assertTrue(rendered.contains("--changeset elias:0001-test"));
            assertTrue(rendered.contains("endDelimiter:;"));
            assertTrue(rendered.contains("--rollback"));
            assertTrue(rendered.contains("data is not restored"));

            List<String> rollbackLines = ROLLBACK.matcher(rendered).results()
                    .map(m -> normalize(m.group(1)))
                    .toList();
            assertEquals(expectedRollbackOrder(diff.getChanges()), rollbackLines);
        }
    }

    @Test
    void createTableRollbackIsDropTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:elias_create;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "")) {
            TableSpec expected = new TableSpec().setName("fresh");
            expected.getColumns().add(new ColumnSpec()
                    .setName("id")
                    .setDataType("bigint")
                    .setColumnType("bigint")
                    .setNullable(false)
                    .setPrimaryKey(true));

            DiffResult diff = new ExportDiffer(
                    connection,
                    new MySQL57Generator().setDropIfExists(false)).diff(List.of(expected));
            assertEquals(1, diff.getChanges().size());
            ExportChange create = diff.getChanges().get(0);
            assertEquals(ExportChange.Kind.CREATE_TABLE, create.getKind());
            assertTrue(create.getRollbackSql().toLowerCase().contains("drop table"));

            String rendered = new LiquibaseSqlExporter().render(diff, "0002-create", "elias");
            Matcher matcher = ROLLBACK.matcher(rendered);
            assertTrue(matcher.find());
            assertTrue(matcher.group(1).toLowerCase().contains("drop table"));
        }
    }

    @Test
    void exportsCreateIndexWhenMissing() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50))");
            TableSpec expected = demoTable();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name")
                    .setUnique(false)
                    .setColumns("name ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            List<ExportChange> indexChanges = indexChanges(diff);
            assertEquals(1, indexChanges.size());
            assertEquals(ExportChange.Kind.CREATE_INDEX, indexChanges.get(0).getKind());
            assertTrue(indexChanges.get(0).getSql().toLowerCase().contains("create index"));
            assertTrue(indexChanges.get(0).getRollbackSql().toLowerCase().contains("drop index"));
        }
    }

    @Test
    void exportsDropAndCreateWhenIndexDefinitionChanges() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50) not null)");
            st.execute("create index idx_name on demo (name)");
            TableSpec expected = demoTable();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name")
                    .setUnique(true)
                    .setColumns("name ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            List<ExportChange> indexChanges = indexChanges(diff);
            assertTrue(indexChanges.stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.DROP_INDEX));
            assertTrue(indexChanges.stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.CREATE_INDEX
                            && c.getSql().toLowerCase().contains("unique")));
        }
    }

    @Test
    void exportsDropExtraIndex() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50))");
            st.execute("create index idx_obsolete on demo (name)");

            DiffResult diff = differ(connection).diff(List.of(demoTable()));
            List<ExportChange> extraDrops = diff.getChanges().stream()
                    .filter(c -> c.getKind() == ExportChange.Kind.DROP_INDEX)
                    .filter(c -> c.getSummary().startsWith("drop extra index"))
                    .toList();
            assertEquals(1, extraDrops.size());
            assertTrue(extraDrops.get(0).getSql().toLowerCase().contains("idx_obsolete"));
            assertTrue(extraDrops.get(0).getRollbackSql().toLowerCase().contains("create"));
        }
    }

    @Test
    void doesNotExportWhenIndexAlreadyMatches() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50))");
            st.execute("create index idx_name on demo (name)");
            TableSpec expected = demoTable();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name")
                    .setUnique(false)
                    .setColumns("name ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).isEmpty());
        }
    }

    @Test
    void doesNotExportWhenH2RenamesInlineUniqueKey() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("""
                    create table demo (
                      id bigint not null primary key,
                      account_id varchar(64) not null,
                      unique key uk_gdt_account_id (account_id)
                    )
                    """);
            TableSpec expected = new TableSpec().setName("demo");
            expected.getColumns().add(new ColumnSpec()
                    .setName("id")
                    .setDataType("bigint")
                    .setColumnType("bigint")
                    .setNullable(false)
                    .setPrimaryKey(true));
            expected.getColumns().add(new ColumnSpec()
                    .setName("account_id")
                    .setDataType("varchar")
                    .setColumnType("varchar(64)")
                    .setLength(64L)
                    .setNullable(false));
            expected.getIndexes().add(new IndexSpec()
                    .setName("uk_gdt_account_id")
                    .setUnique(true)
                    .setColumns("account_id ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).isEmpty(),
                    () -> "unexpected index changes: " + indexChanges(diff).stream()
                            .map(ExportChange::getSummary)
                            .toList());
        }
    }

    @Test
    void doesNotExportWhenSeparateUniqueIndexNamePreserved() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("""
                    create table demo (
                      id bigint not null primary key,
                      account_id varchar(64) not null
                    )
                    """);
            st.execute("create unique index uk_gdt_account_id on demo (account_id)");
            TableSpec expected = new TableSpec().setName("demo");
            expected.getColumns().add(new ColumnSpec()
                    .setName("id")
                    .setDataType("bigint")
                    .setColumnType("bigint")
                    .setNullable(false)
                    .setPrimaryKey(true));
            expected.getColumns().add(new ColumnSpec()
                    .setName("account_id")
                    .setDataType("varchar")
                    .setColumnType("varchar(64)")
                    .setLength(64L)
                    .setNullable(false));
            expected.getIndexes().add(new IndexSpec()
                    .setName("uk_gdt_account_id")
                    .setUnique(true)
                    .setColumns("account_id ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).isEmpty());
        }
    }

    @Test
    void recreatesWhenH2AliasDefinitionDiffers() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("""
                    create table demo (
                      id bigint not null primary key,
                      account_id varchar(64) not null,
                      unique key uk_gdt_account_id (account_id)
                    )
                    """);
            TableSpec expected = new TableSpec().setName("demo");
            expected.getColumns().add(new ColumnSpec()
                    .setName("id")
                    .setDataType("bigint")
                    .setColumnType("bigint")
                    .setNullable(false)
                    .setPrimaryKey(true));
            expected.getColumns().add(new ColumnSpec()
                    .setName("account_id")
                    .setDataType("varchar")
                    .setColumnType("varchar(64)")
                    .setLength(64L)
                    .setNullable(false));
            // same H2 alias target name, but expect non-unique → recreate
            expected.getIndexes().add(new IndexSpec()
                    .setName("uk_gdt_account_id")
                    .setUnique(false)
                    .setColumns("account_id ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.DROP_INDEX));
            assertTrue(indexChanges(diff).stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.CREATE_INDEX));
        }
    }

    @Test
    void exportsCreateIndexFromFieldAnnotations() throws Exception {
        TableSpec expected = SpecMaker.makeTableSpec(IndexedAccount.class);
        assertTrue(expected.getIndexes().stream()
                .anyMatch(index -> "idx_username".equals(index.getName())));
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table `" + expected.getName()
                    + "` (id bigint not null primary key, username varchar(255) not null)");
            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(diff.getChanges().stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.CREATE_INDEX
                            && c.getSummary().contains("idx_username")));
        }
    }

    @EliasTable
    private static class IndexedAccount {
        @TableId(type = IdType.AUTO)
        private Long id;
        @Index(name = "idx_username")
        @NotNull
        private String username;
    }

    private static Connection h2() throws Exception {
        return DriverManager.getConnection(
                "jdbc:h2:mem:elias_idx_" + System.nanoTime()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "");
    }

    private static ExportDiffer differ(Connection connection) throws Exception {
        return new ExportDiffer(connection, new MySQL57Generator().setDropIfExists(false));
    }

    private static TableSpec demoTable() {
        TableSpec expected = new TableSpec().setName("demo");
        expected.getColumns().add(new ColumnSpec()
                .setName("id")
                .setDataType("bigint")
                .setColumnType("bigint")
                .setNullable(false)
                .setPrimaryKey(true));
        expected.getColumns().add(new ColumnSpec()
                .setName("name")
                .setDataType("varchar")
                .setColumnType("varchar(50)")
                .setLength(50L)
                .setNullable(true));
        return expected;
    }

    @Test
    void skipsCreateWhenExistingIndexCoversExpected() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50), email varchar(50))");
            st.execute("create index idx_name_email on demo (name, email)");
            TableSpec expected = demoTableWithEmail();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name")
                    .setUnique(false)
                    .setColumns("name ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).isEmpty());
        }
    }

    @Test
    void exportsRenameWhenExactMatchHasDifferentName() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50))");
            st.execute("create index idx_legacy on demo (name)");
            TableSpec expected = demoTable();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name")
                    .setUnique(false)
                    .setColumns("name ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            List<ExportChange> indexChanges = indexChanges(diff);
            assertEquals(1, indexChanges.size());
            assertEquals(ExportChange.Kind.RENAME_INDEX, indexChanges.get(0).getKind());
            assertTrue(indexChanges.get(0).getSql().toLowerCase().contains("rename index"));
            assertTrue(indexChanges.get(0).getRollbackSql().toLowerCase().contains("rename index"));
            assertFalse(indexChanges.get(0).isDestructive());
        }
    }

    @Test
    void exportsCreateAndDropExtraWhenRenamedIndexNoLongerMatchesColumns() throws Exception {
        try (Connection connection = h2();
             Statement st = connection.createStatement()) {
            st.execute("create table demo (id bigint not null primary key, name varchar(50), email varchar(50))");
            st.execute("create index idx_legacy on demo (name)");
            TableSpec expected = demoTableWithEmail();
            expected.getIndexes().add(new IndexSpec()
                    .setName("idx_name_email")
                    .setUnique(false)
                    .setColumns("name ASC, email ASC"));

            DiffResult diff = differ(connection).diff(List.of(expected));
            assertTrue(indexChanges(diff).stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.CREATE_INDEX
                            && c.getSql().toLowerCase().contains("idx_name_email")));
            assertTrue(indexChanges(diff).stream()
                    .anyMatch(c -> c.getKind() == ExportChange.Kind.DROP_INDEX
                            && c.getSummary().startsWith("drop extra index")
                            && c.getSql().toLowerCase().contains("idx_legacy")));
        }
    }

    private static TableSpec demoTableWithEmail() {
        TableSpec expected = demoTable();
        expected.getColumns().add(new ColumnSpec()
                .setName("email")
                .setDataType("varchar")
                .setColumnType("varchar(50)")
                .setLength(50L)
                .setNullable(true));
        return expected;
    }

    private static List<ExportChange> indexChanges(DiffResult diff) {
        return diff.getChanges().stream()
                .filter(c -> c.getKind() == ExportChange.Kind.CREATE_INDEX
                        || c.getKind() == ExportChange.Kind.DROP_INDEX
                        || c.getKind() == ExportChange.Kind.RENAME_INDEX)
                .toList();
    }

    private static List<String> expectedRollbackOrder(List<ExportChange> changes) {
        List<String> expected = new ArrayList<>();
        for (int i = changes.size() - 1; i >= 0; i--) {
            String rollback = changes.get(i).getRollbackSql();
            for (String part : rollback.split(";")) {
                String sql = part.trim();
                if (!sql.isEmpty()) {
                    expected.add(normalize(sql));
                }
            }
        }
        return expected;
    }

    private static String normalize(String sql) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed.toLowerCase().replaceAll("\\s+", " ");
    }
}
