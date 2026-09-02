package cc.ddrpa.dorian.elias.generator.export;

/**
 * One SQL fragment produced by export diff, with optional Liquibase rollback SQL.
 */
public class ExportChange {

    public enum Kind {
        CREATE_TABLE,
        ADD_COLUMN,
        MODIFY_COLUMN,
        DROP_COLUMN,
        DROP_TABLE,
        CREATE_INDEX,
        DROP_INDEX,
        RENAME_INDEX
    }

    private final Kind kind;
    private final String summary;
    private final String sql;
    private final String rollbackSql;
    private final boolean destructive;

    public ExportChange(Kind kind, String summary, String sql, boolean destructive) {
        this(kind, summary, sql, null, destructive);
    }

    public ExportChange(Kind kind, String summary, String sql, String rollbackSql,
                        boolean destructive) {
        this.kind = kind;
        this.summary = summary;
        this.sql = sql;
        this.rollbackSql = rollbackSql;
        this.destructive = destructive;
    }

    public Kind getKind() {
        return kind;
    }

    public String getSummary() {
        return summary;
    }

    public String getSql() {
        return sql;
    }

    /**
     * Structural inverse SQL for Liquibase {@code --rollback}, or null if none.
     */
    public String getRollbackSql() {
        return rollbackSql;
    }

    public boolean isDestructive() {
        return destructive;
    }
}
