package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpecBuilder;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ColumnCommentValidationTest {

    @Test
    void shouldFlagCommentOnlyWhenDatabaseCommentEmpty() {
        ColumnProperties properties = new ColumnProperties(row("name", null));
        ColumnSpec spec = new ColumnSpec()
                .setName("name")
                .setDataType("varchar")
                .setLength(255L)
                .setComment("display name");

        Optional<ColumnSpecMismatch> mismatch = properties.validate(spec);
        Assertions.assertTrue(mismatch.isPresent());
        Assertions.assertTrue(mismatch.get().isCommentMismatch());
        Assertions.assertEquals("display name", mismatch.get().getExpectedComment());
    }

    @Test
    void shouldNotFlagWhenDatabaseAlreadyHasComment() {
        ColumnProperties properties = new ColumnProperties(row("name", "existing"));
        ColumnSpec spec = new ColumnSpec()
                .setName("name")
                .setDataType("varchar")
                .setLength(255L)
                .setComment("from schema");

        Assertions.assertTrue(properties.validate(spec).isEmpty());
    }

    @Test
    void shouldPreserveExistingCommentOnModify() {
        ColumnProperties properties = new ColumnProperties(row("name", "keep-me"));
        ColumnSpec spec = new ColumnSpec()
                .setName("name")
                .setDataType("varchar")
                .setLength(100L)
                .setComment("ignored");

        ColumnSpecMismatch mismatch = properties.validate(spec).orElseThrow()
                .setTableName("t")
                .setColumnName("name");
        ColumnModifySpec modifySpec = ColumnModifySpecBuilder.build(mismatch);
        Assertions.assertEquals("keep-me", modifySpec.getComment());
        Assertions.assertFalse(modifySpec.isAlterComment());
    }

    @Test
    void shouldFillCommentOnModifyWhenDatabaseEmpty() {
        ColumnProperties properties = new ColumnProperties(row("name", null));
        ColumnSpec spec = new ColumnSpec()
                .setName("name")
                .setDataType("varchar")
                .setLength(255L)
                .setComment("new comment");

        ColumnSpecMismatch mismatch = properties.validate(spec).orElseThrow()
                .setTableName("t")
                .setColumnName("name");
        ColumnModifySpec modifySpec = ColumnModifySpecBuilder.build(mismatch);
        Assertions.assertEquals("new comment", modifySpec.getComment());
        Assertions.assertTrue(modifySpec.isAlterComment());
    }

    private static Map<String, Object> row(String name, String comment) {
        Map<String, Object> row = new HashMap<>();
        row.put("COLUMN_NAME", name);
        row.put("COLUMN_DEFAULT", null);
        row.put("IS_NULLABLE", "YES");
        row.put("DATA_TYPE", "varchar");
        row.put("CHARACTER_MAXIMUM_LENGTH", 255L);
        row.put("COLUMN_TYPE", "varchar(255)");
        row.put("COLUMN_COMMENT", comment);
        return row;
    }
}
