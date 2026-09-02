package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * Expected index name is missing, but an equivalent index exists under another name.
 */
public class IndexRenameMismatch implements ISpecMismatch {

    private final String tableName;
    private final String actualName;
    private final IndexSpec expectedIndexSpec;

    public IndexRenameMismatch(String tableName, String actualName, IndexSpec expectedIndexSpec) {
        this.tableName = tableName;
        this.actualName = actualName;
        this.expectedIndexSpec = expectedIndexSpec;
    }

    public String getTableName() {
        return tableName;
    }

    public String getActualName() {
        return actualName;
    }

    public IndexSpec getExpectedIndexSpec() {
        return expectedIndexSpec;
    }

    @Override
    public String errorMessage() {
        return String.format(
                "Expect index `%s` in table `%s` but found equivalent index `%s`.",
                expectedIndexSpec.getName(), tableName, actualName);
    }
}
