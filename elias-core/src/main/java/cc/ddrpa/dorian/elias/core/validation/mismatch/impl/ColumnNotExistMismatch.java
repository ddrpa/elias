package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

public class ColumnNotExistMismatch implements ISpecMismatch {

    private String tableName;
    private ColumnSpec columnSpec;

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