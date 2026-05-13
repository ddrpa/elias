package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;

public class ReservedKeywordMismatch implements ISpecMismatch {

    private final String tableName;
    private final String subject;
    private final String detail;

    public ReservedKeywordMismatch(String tableName, String subject, String detail) {
        this.tableName = tableName;
        this.subject = subject;
        this.detail = detail;
    }

    @Override
    public String errorMessage() {
        return String.format("Invalid schema definition in table `%s`: %s uses reserved keyword. %s",
                tableName, subject, detail);
    }
}
