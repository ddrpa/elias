package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引规格不匹配
 */
public class IndexSpecMismatch implements ISpecMismatch {

    private String tableName;
    private String indexName;
    private boolean uniqueMismatch = false;
    private boolean columnsMismatch = false;
    private boolean expectedUnique;
    private boolean actualUnique;
    private List<String> expectedColumns;
    private List<String> actualColumns;
    private IndexSpec expectedIndexSpec;

    public IndexSpecMismatch addUniqueMismatch(boolean expectedUnique, boolean actualUnique) {
        this.uniqueMismatch = true;
        this.expectedUnique = expectedUnique;
        this.actualUnique = actualUnique;
        return this;
    }

    public IndexSpecMismatch addColumnsMismatch(List<String> expectedColumns,
                                                List<String> actualColumns) {
        this.columnsMismatch = true;
        this.expectedColumns = expectedColumns;
        this.actualColumns = actualColumns;
        return this;
    }

    public IndexSpecMismatch setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public IndexSpecMismatch setIndexName(String indexName) {
        this.indexName = indexName;
        return this;
    }

    public IndexSpecMismatch setExpectedIndexSpec(IndexSpec expectedIndexSpec) {
        this.expectedIndexSpec = expectedIndexSpec;
        return this;
    }

    public IndexSpec getExpectedIndexSpec() {
        return expectedIndexSpec;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIndexName() {
        return indexName;
    }

    @Override
    public String errorMessage() {
        List<String> details = new ArrayList<>();
        if (uniqueMismatch) {
            details.add(String.format("* Different unique property: expected '%s', actual '%s'",
                    expectedUnique, actualUnique));
        }
        if (columnsMismatch) {
            details.add(String.format("* Different column list: expected '%s', actual '%s'",
                    String.join(", ", expectedColumns), String.join(", ", actualColumns)));
        }
        String detail = String.join("\n", details);
        return String.format("Index `%s` in table `%s` has specification mismatch:\n%s",
                indexName, tableName, detail);
    }
}
