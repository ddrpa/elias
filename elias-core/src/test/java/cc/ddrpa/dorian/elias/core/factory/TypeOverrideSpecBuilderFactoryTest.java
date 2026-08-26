package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.SpecMaker;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.TypeOverride;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnTypeParser;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.validation.ColumnProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TypeOverrideSpecBuilderFactoryTest {

    @Test
    void shouldParseLengthEmbeddedInTypeString() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(EmbeddedLengthEntity.class);
        ColumnSpec column = findColumn(tableSpec, "last_login_ip");
        Assertions.assertEquals("varbinary", column.getDataType());
        Assertions.assertEquals(16L, column.getLength());
        Assertions.assertEquals("varbinary(16)", column.getColumnType());
    }

    @Test
    void shouldPreferExplicitLengthAttributeOverEmbeddedLength() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(ExplicitLengthEntity.class);
        ColumnSpec column = findColumn(tableSpec, "payload");
        Assertions.assertEquals("varchar", column.getDataType());
        Assertions.assertEquals(500L, column.getLength());
        Assertions.assertEquals("varchar(500)", column.getColumnType());
    }

    @Test
    void shouldPreserveUnsignedModifierInColumnType() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(UnsignedEntity.class);
        ColumnSpec column = findColumn(tableSpec, "amount");
        Assertions.assertEquals("bigint", column.getDataType());
        Assertions.assertEquals(20L, column.getLength());
        Assertions.assertEquals("bigint(20) unsigned", column.getColumnType());
    }

    @Test
    void shouldMatchInformationSchemaWhenTypeHadEmbeddedLength() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(EmbeddedLengthEntity.class);
        ColumnSpec column = findColumn(tableSpec, "last_login_ip");

        Map<String, Object> row = new HashMap<>();
        row.put("COLUMN_NAME", "last_login_ip");
        row.put("COLUMN_DEFAULT", null);
        row.put("IS_NULLABLE", "YES");
        row.put("DATA_TYPE", "varbinary");
        row.put("CHARACTER_MAXIMUM_LENGTH", 16L);
        row.put("COLUMN_TYPE", "varbinary(16)");

        Assertions.assertTrue(new ColumnProperties(row).validate(column).isEmpty());
    }

    @Test
    void shouldParseDecimalPrecisionAndScale() {
        ColumnTypeParser.Parsed parsed = ColumnTypeParser.parse("decimal(10, 2)");
        Assertions.assertEquals("decimal", parsed.dataType());
        Assertions.assertEquals(Optional.of(10), parsed.precision());
        Assertions.assertEquals(Optional.of(2), parsed.scale());
        Assertions.assertEquals(Optional.empty(), parsed.length());
    }

    private static ColumnSpec findColumn(TableSpec tableSpec, String name) {
        return tableSpec.getColumns().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow();
    }

    @EliasTable
    @SuppressWarnings("unused")
    private static class EmbeddedLengthEntity {
        @TypeOverride(type = "VARBINARY(16)")
        private byte[] lastLoginIp;
    }

    @EliasTable
    @SuppressWarnings("unused")
    private static class ExplicitLengthEntity {
        @TypeOverride(type = "varchar(255)", length = 500)
        private String payload;
    }

    @EliasTable
    @SuppressWarnings("unused")
    private static class UnsignedEntity {
        @TypeOverride(type = "bigint(20) unsigned")
        private Long amount;
    }
}
