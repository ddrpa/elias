package cc.ddrpa.dorian.elias.maven;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards Liquibase + H2 {@code DATABASE_TO_LOWER} / sticky {@code mem:} interactions.
 */
class H2LiquibaseDatabaseToLowerTest {

    @TempDir
    Path dir;

    @Test
    void uniqueH2MemUrlAppendsSuffix() {
        String base = "jdbc:h2:mem:elias_export;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        String unique = ChangelogExportMojo.uniqueH2MemUrl(base);
        assertNotEquals(base, unique);
        assertTrue(unique.startsWith("jdbc:h2:mem:elias_export_"));
        assertTrue(unique.endsWith(";MODE=MySQL;DATABASE_TO_LOWER=TRUE"));
    }

    @Test
    void liquibaseUpdateSucceedsOnFreshMemWithDatabaseToLower() throws Exception {
        Path master = writeMaster();
        String url = ChangelogExportMojo.uniqueH2MemUrl(
                "jdbc:h2:mem:elias_lb_ok;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                        + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            setUnquotedLower((H2Database) database);
            Liquibase liquibase = new Liquibase(
                    master.getFileName().toString(),
                    new DirectoryResourceAccessor(dir.toFile()),
                    database);
            assertDoesNotThrow(() -> liquibase.update(new Contexts(), new LabelExpression()));
        }
    }

    @Test
    void stickyMemSecondConnectionFailsWithoutFreshDatabase() throws Exception {
        Path master = writeMaster();
        String sticky = "jdbc:h2:mem:elias_lb_sticky_case;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(sticky, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            setUnquotedLower((H2Database) database);
            new Liquibase(master.getFileName().toString(),
                    new DirectoryResourceAccessor(dir.toFile()), database)
                    .update(new Contexts(), new LabelExpression());
        }
        try (Connection connection = DriverManager.getConnection(sticky, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            setUnquotedLower((H2Database) database);
            Liquibase liquibase = new Liquibase(
                    master.getFileName().toString(),
                    new DirectoryResourceAccessor(dir.toFile()),
                    database);
            Exception ex = assertThrows(Exception.class,
                    () -> liquibase.update(new Contexts(), new LabelExpression()));
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String message = root.getMessage() == null ? "" : root.getMessage();
            assertTrue(message.toLowerCase().contains("databasechangelog"), message);
            assertTrue(message.toLowerCase().contains("already exists"), message);
        }
    }

    private Path writeMaster() throws Exception {
        Path master = dir.resolve("db.changelog-master.yaml");
        Files.writeString(master, """
                databaseChangeLog:
                  - changeSet:
                      id: 1
                      author: t
                      changes:
                        - createTable:
                            tableName: demo
                            columns:
                              - column:
                                  name: id
                                  type: BIGINT
                                  constraints:
                                    primaryKey: true
                """);
        return master;
    }

    private static void setUnquotedLower(H2Database database) throws Exception {
        Class<?> type = database.getClass();
        Field field = null;
        while (type != null) {
            try {
                field = type.getDeclaredField("unquotedObjectsAreUppercased");
                break;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException("unquotedObjectsAreUppercased");
        }
        field.setAccessible(true);
        field.set(database, Boolean.FALSE);
    }
}
