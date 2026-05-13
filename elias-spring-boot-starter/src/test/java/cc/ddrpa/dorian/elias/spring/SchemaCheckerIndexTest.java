package cc.ddrpa.dorian.elias.spring;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class SchemaCheckerIndexTest {

    @Test
    void shouldSkipWhenIndexAlreadyMatches() throws Exception {
        JdbcTemplate jdbcTemplate = mockJdbcTemplate();
        mockMetadataRows(jdbcTemplate, mockIndexRows(1, "A"));
        SchemaChecker schemaChecker = new SchemaChecker(jdbcTemplate)
                .setAutoFix(true)
                .addTableSpecies(List.of(mockTableSpec("username ASC", false)));

        schemaChecker.check();

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void shouldDropAndRecreateChangedIndexWhenAutoFixEnabled() throws Exception {
        JdbcTemplate jdbcTemplate = mockJdbcTemplate();
        mockMetadataRows(jdbcTemplate, mockIndexRows(1, "D"));
        SchemaChecker schemaChecker = new SchemaChecker(jdbcTemplate)
                .setAutoFix(true)
                .addTableSpecies(List.of(mockTableSpec("username ASC", false)));

        schemaChecker.check();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
        Assertions.assertTrue(sqlCaptor.getAllValues().stream()
                .anyMatch(sql -> sql.contains("drop index idx_username on `tbl_account`")));
        Assertions.assertTrue(sqlCaptor.getAllValues().stream()
                .anyMatch(sql -> sql.contains("create index idx_username on `tbl_account` (username ASC)")));
    }

    private static JdbcTemplate mockJdbcTemplate() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("test_db");
        return jdbcTemplate;
    }

    private static void mockMetadataRows(JdbcTemplate jdbcTemplate,
                                         List<Map<String, Object>> indexRows) {
        when(jdbcTemplate.queryForList(startsWith("select COLUMN_NAME"), eq("test_db"), eq("tbl_account")))
                .thenReturn(mockColumnRows());
        when(jdbcTemplate.queryForList(startsWith("select INDEX_NAME"), eq("test_db"), eq("tbl_account")))
                .thenReturn(indexRows);
    }

    private static TableSpec mockTableSpec(String columns, boolean unique) {
        ColumnSpec usernameColumn = new ColumnSpec()
                .setName("username")
                .setDataType("varchar")
                .setLength(255L)
                .setNullable(false);
        IndexSpec usernameIndex = new IndexSpec()
                .setName("idx_username")
                .setColumns(columns)
                .setUnique(unique);
        return new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(usernameColumn))
                .setIndexes(List.of(usernameIndex));
    }

    private static List<Map<String, Object>> mockColumnRows() {
        Map<String, Object> row = new HashMap<>();
        row.put("COLUMN_NAME", "username");
        row.put("COLUMN_DEFAULT", null);
        row.put("IS_NULLABLE", "NO");
        row.put("DATA_TYPE", "varchar");
        row.put("CHARACTER_MAXIMUM_LENGTH", 255L);
        row.put("COLUMN_TYPE", "varchar(255)");
        return List.of(row);
    }

    private static List<Map<String, Object>> mockIndexRows(int nonUnique, String collation) {
        return List.of(Map.of(
                "INDEX_NAME", "idx_username",
                "NON_UNIQUE", nonUnique,
                "SEQ_IN_INDEX", 1,
                "COLUMN_NAME", "username",
                "COLLATION", collation
        ));
    }
}
