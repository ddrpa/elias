package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * 索引不存在不匹配
 */
public class IndexNotExistMismatch implements ISpecMismatch {

    private final String tableName;
    private final IndexSpec indexSpec;

    public IndexNotExistMismatch(String tableName, IndexSpec indexSpec) {
        this.tableName = tableName;
        this.indexSpec = indexSpec;
    }

    public String getTableName() {
        return tableName;
    }

    public IndexSpec getIndexSpec() {
        return indexSpec;
    }

    @Override
    public String errorMessage() {
        return String.format("Expect index `%s` in table `%s` but not found.",
                indexSpec.getName(), tableName);
    }
}
