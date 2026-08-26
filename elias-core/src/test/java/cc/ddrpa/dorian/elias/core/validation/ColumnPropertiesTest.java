package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnTypeParser;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ColumnPropertiesTest {

    @Test
    void shouldNotMismatchWhenLengthOnlyPresentInColumnType() {
        Map<String, Object> row = baseRow("totp_secret", "varbinary", "varbinary(512)");
        row.put("CHARACTER_MAXIMUM_LENGTH", null);

        ColumnProperties properties = new ColumnProperties(row);
        Assertions.assertEquals(Optional.of(512L), properties.getDataLength());

        ColumnSpec spec = new ColumnSpec()
                .setName("totp_secret")
                .setDataType("varbinary")
                .setLength(512L);

        Assertions.assertTrue(properties.validate(spec).isEmpty());
    }

    @Test
    void shouldPreferCharacterMaximumLengthOverColumnType() {
        Map<String, Object> row = baseRow("payload", "varbinary", "varbinary(512)");
        row.put("CHARACTER_MAXIMUM_LENGTH", 256L);

        ColumnProperties properties = new ColumnProperties(row);
        Assertions.assertEquals(Optional.of(256L), properties.getDataLength());

        ColumnSpec spec = new ColumnSpec()
                .setName("payload")
                .setDataType("varbinary")
                .setLength(512L);

        Optional<ColumnSpecMismatch> mismatch = properties.validate(spec);
        Assertions.assertTrue(mismatch.isPresent());
        String message = mismatch.get()
                .setTableName("sys_user")
                .setColumnName("payload")
                .errorMessage();
        Assertions.assertTrue(message.contains("Column length not match: expected 512, actual 256"));
        Assertions.assertFalse(message.contains("Column type not match: expected 'varbinary(512)', actual 'varbinary(512)'"));
    }

    @Test
    void shouldParseLengthFromColumnType() {
        Assertions.assertEquals(Optional.of(512L),
                ColumnTypeParser.parseLengthFromColumnType("varbinary(512)"));
        Assertions.assertEquals(Optional.of(255L),
                ColumnTypeParser.parseLengthFromColumnType("varchar(255)"));
        Assertions.assertEquals(Optional.of(10L),
                ColumnTypeParser.parseLengthFromColumnType("decimal(10, 2)"));
        Assertions.assertEquals(Optional.empty(),
                ColumnTypeParser.parseLengthFromColumnType("blob"));
    }

    private static Map<String, Object> baseRow(String name, String dataType, String columnType) {
        Map<String, Object> row = new HashMap<>();
        row.put("COLUMN_NAME", name);
        row.put("COLUMN_DEFAULT", null);
        row.put("IS_NULLABLE", "YES");
        row.put("DATA_TYPE", dataType);
        row.put("COLUMN_TYPE", columnType);
        row.put("COLUMN_COMMENT", null);
        return row;
    }
}
