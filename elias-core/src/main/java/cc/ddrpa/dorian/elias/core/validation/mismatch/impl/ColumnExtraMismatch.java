package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * 数据库中存在实体未声明的列（导出时可生成 DROP COLUMN）
 */
public class ColumnExtraMismatch implements ISpecMismatch {

    private final String tableName;
    private final String columnName;

    public ColumnExtraMismatch(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public String errorMessage() {
        return String.format("Column `%s` exists in table `%s` but is not declared on the entity.",
                columnName, tableName);
    }
}
