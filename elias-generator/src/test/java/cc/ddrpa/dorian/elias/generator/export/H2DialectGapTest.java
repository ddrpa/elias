package cc.ddrpa.dorian.elias.generator.export;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents H2 MODE=MySQL gaps relevant to export baseline replay and rollback SQL dialect.
 */
class H2DialectGapTest {

    private static String url(String dbName) {
        return "jdbc:h2:mem:" + dbName
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    }

    @Test
    void modifyAndDropIndexWorkUnderH2MySqlMode() throws Exception {
        try (Connection c = DriverManager.getConnection(url("gap_modify"), "sa", "");
             Statement st = c.createStatement()) {
            st.execute("create table `t` (`id` bigint, `name` varchar(50))");
            st.execute("create index idx_name on `t` (`name`)");
            assertDoesNotThrow(() -> st.execute(
                    "alter table `t` modify column `name` varchar(100) null"));
            assertDoesNotThrow(() -> st.execute("drop index `idx_name` on `t`"));
        }
    }

    @Test
    void changeColumnAcceptedButPreferMysqlForRenameReplay() throws Exception {
        try (Connection c = DriverManager.getConnection(url("gap_change"), "sa", "");
             Statement st = c.createStatement()) {
            st.execute("create table `t` (`id` bigint, `name` varchar(50))");
            // H2 2.x MODE=MySQL often accepts CHANGE; still not a substitute for MySQL rename semantics
            assertDoesNotThrow(() -> st.execute(
                    "alter table `t` change column `name` `name2` varchar(100) null"));
        }
    }

    @Test
    void decoratedMysqlDumpStyleStillFailsOnH2() throws Exception {
        try (Connection c = DriverManager.getConnection(url("gap_baseline"), "sa", "");
             Statement st = c.createStatement()) {
            // Legacy dump decorations (why baseline was stripped to a portable subset)
            assertThrows(SQLException.class, () -> st.execute("""
                    create table sys_x (
                      id bigint not null,
                      name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'n',
                      PRIMARY KEY (id) USING BTREE
                    ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 't' ROW_FORMAT = DYNAMIC
                    """));
        }
    }

    @Test
    void catalogSchemaScopedMetadataFindsColumns() throws Exception {
        try (Connection c = DriverManager.getConnection(url("gap_scope"), "sa", "");
             Statement st = c.createStatement()) {
            st.execute("create table `scoped` (`id` bigint not null)");
            DatabaseMetaData md = c.getMetaData();
            int count = 0;
            try (ResultSet rs = md.getColumns(c.getCatalog(), c.getSchema(), "scoped", null)) {
                while (rs.next()) {
                    count++;
                }
            }
            assertTrue(count >= 1,
                    "expected columns via catalog=" + c.getCatalog() + " schema=" + c.getSchema());
        }
    }
}
