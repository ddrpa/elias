package cc.ddrpa.dorian.elias.generator.export;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.generator.MySQL57Generator;
import org.junit.jupiter.api.Test;

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
