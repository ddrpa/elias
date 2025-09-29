package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

/**
 * 表不存在不匹配
 */
public class TableNotExistMismatch implements ISpecMismatch {

    private final TableSpec expectedTableSpec;

    public TableNotExistMismatch(TableSpec expectedTableSpec) {
        this.expectedTableSpec = expectedTableSpec;
    }

    public TableSpec getExpectedTableSpec() {
        return expectedTableSpec;
    }

    @Override
    public String errorMessage() {
        return String.format("Expect table `%s` but not found.",
                expectedTableSpec.getName());
    }
}