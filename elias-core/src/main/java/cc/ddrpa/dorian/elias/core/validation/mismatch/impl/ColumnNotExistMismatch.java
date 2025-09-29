package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * 列不存在不匹配
 */
public class ColumnNotExistMismatch implements ISpecMismatch {

    private final String tableName;
    private final ColumnSpec columnSpec;

    public ColumnNotExistMismatch(String tableName, ColumnSpec columnSpec) {
        this.tableName = tableName;
        this.columnSpec = columnSpec;
    }

    public String getTableName() {
        return tableName;
    }

    public ColumnSpec getColumnSpec() {
        return columnSpec;
    }

    @Override
    public String errorMessage() {
        return String.format("Expect column `%s` in table `%s` but not found.",
                columnSpec.getName(), tableName);
    }
}