package cc.ddrpa.dorian.elias.generator.export;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.generator.MySQL57Generator;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards H2 MODE=MySQL metadata vs Elias Spec comparison (false MODIFY noise).
 */
class H2MysqlCompatTest {

    private static final String H2_URL =
            "jdbc:h2:mem:elias_compat;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    @Test
    void mysqlStyleCreateTableThenMatchingSpecProducesNoDiff() throws Exception {
        try (Connection connection = DriverManager.getConnection(H2_URL, "sa", "");
             Statement st = connection.createStatement()) {
            // Mimic exported MySQL DDL applied under H2 MODE=MySQL
            st.execute("""
                    create table `demo_compat` (
                      `id` bigint not null primary key,
                      `name` varchar(50) null,
                      `created_at` datetime null
                    )
                    """);

            TableSpec expected = new TableSpec().setName("demo_compat");
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
            expected.getColumns().add(new ColumnSpec()
                    .setName("created_at")
                    .setDataType("datetime")
                    .setColumnType("datetime")
                    .setNullable(true));

            DiffResult diff = new ExportDiffer(
                    connection,
                    new MySQL57Generator().setDropIfExists(false)).diff(List.of(expected));

            assertTrue(diff.isEmpty(),
                    () -> "unexpected changes: " + new LiquibaseSqlExporter()
                            .render(diff, "x", "t"));
        }
    }

    @Test
    void normalizerMapsH2TypeNames() {
        assertEquals("varchar", JdbcTypeNormalizer.normalizeDataType("CHARACTER VARYING"));
        assertEquals("datetime", JdbcTypeNormalizer.normalizeDataType("TIMESTAMP"));
        assertEquals("bigint", JdbcTypeNormalizer.normalizeDataType("BIGINT"));
        assertEquals("int", JdbcTypeNormalizer.normalizeDataType("INTEGER"));
        assertEquals("x", JdbcTypeNormalizer.normalizeDefault("'x'"));
        assertEquals(null, JdbcTypeNormalizer.normalizeDefault("NULL"));
    }
}
