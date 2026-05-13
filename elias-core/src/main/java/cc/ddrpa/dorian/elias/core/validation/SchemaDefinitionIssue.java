package cc.ddrpa.dorian.elias.core.validation;

public class SchemaDefinitionIssue {

    public enum Type {
        RESERVED_KEYWORD,
        ILLEGAL_IDENTIFIER,
        INVALID_INDEX_DEFINITION
    }

    private final Type type;
    private final String subject;
    private final String detail;

    public SchemaDefinitionIssue(Type type, String subject, String detail) {
        this.type = type;
        this.subject = subject;
        this.detail = detail;
    }

    public Type getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getDetail() {
        return detail;
    }
}
