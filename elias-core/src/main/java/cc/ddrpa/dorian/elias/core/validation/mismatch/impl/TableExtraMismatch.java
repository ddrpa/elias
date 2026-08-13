package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * 数据库中存在实体未声明的表（导出时可生成 DROP TABLE）
 */
public class TableExtraMismatch implements ISpecMismatch {

    private final String tableName;

    public TableExtraMismatch(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String errorMessage() {
        return String.format("Table `%s` exists in database but is not declared on scanned entities.",
                tableName);
    }
}
