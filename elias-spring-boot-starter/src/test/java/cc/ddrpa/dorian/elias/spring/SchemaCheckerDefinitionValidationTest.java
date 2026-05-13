package cc.ddrpa.dorian.elias.spring;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class SchemaCheckerDefinitionValidationTest {

    @Test
    void shouldReturnTrueAndSkipMetadataQueryWhenDefinitionInvalid() throws Exception {
        JdbcTemplate jdbcTemplate = mockJdbcTemplate();
        TableSpec tableSpec = new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(new ColumnSpec().setName("order")
                        .setDataType("varchar").setLength(255L)))
                .setIndexes(List.of(new IndexSpec().setName("idx_order").setColumns("order ASC")));

        boolean hasMismatch = new SchemaChecker(jdbcTemplate)
                .addTableSpecies(List.of(tableSpec))
                .check();

        Assertions.assertTrue(hasMismatch);
        verify(jdbcTemplate, never()).queryForList(startsWith("select COLUMN_NAME"), eq("test_db"),
                eq("tbl_account"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void shouldReturnFalseWhenDefinitionAndMetadataAreValid() throws Exception {
        JdbcTemplate jdbcTemplate = mockJdbcTemplate();
        when(jdbcTemplate.queryForList(startsWith("select COLUMN_NAME"), eq("test_db"), eq("tbl_account")))
                .thenReturn(mockColumnRows());
        when(jdbcTemplate.queryForList(startsWith("select INDEX_NAME"), eq("test_db"), eq("tbl_account")))
                .thenReturn(mockIndexRows());
        TableSpec tableSpec = new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(new ColumnSpec().setName("username")
                        .setDataType("varchar").setLength(255L).setNullable(false)))
                .setIndexes(List.of(new IndexSpec().setName("idx_username")
                        .setColumns("username ASC")
                        .setUnique(false)));

        boolean hasMismatch = new SchemaChecker(jdbcTemplate)
                .addTableSpecies(List.of(tableSpec))
                .check();

        Assertions.assertFalse(hasMismatch);
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

    private static List<Map<String, Object>> mockIndexRows() {
        return List.of(Map.of(
                "INDEX_NAME", "idx_username",
                "NON_UNIQUE", 1,
                "SEQ_IN_INDEX", 1,
                "COLUMN_NAME", "username",
                "COLLATION", "A"
        ));
    }
}
