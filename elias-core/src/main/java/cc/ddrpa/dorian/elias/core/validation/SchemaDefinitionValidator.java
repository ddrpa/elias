package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SchemaDefinitionValidator {

    private static final int MAX_IDENTIFIER_LENGTH = 64;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "ADD", "ALTER", "AND", "ASC", "BY", "CREATE", "DEFAULT", "DELETE",
            "DESC", "DROP", "FROM", "GROUP", "INDEX", "INSERT", "INTO", "JOIN",
            "KEY", "NOT", "NULL", "ON", "OR", "ORDER", "PRIMARY", "SELECT",
            "SET", "TABLE", "UNION", "UNIQUE", "UPDATE", "VALUES", "WHERE"
    );

    public List<SchemaDefinitionIssue> validate(TableSpec tableSpec) {
        List<SchemaDefinitionIssue> issues = new ArrayList<>();
        validateIdentifier("table", tableSpec.getName(), issues);
        Set<String> columnNames = tableSpec.getColumns().stream()
                .map(ColumnSpec::getName)
                .collect(Collectors.toSet());
        for (ColumnSpec columnSpec : tableSpec.getColumns()) {
            validateIdentifier("column", columnSpec.getName(), issues);
        }
        Set<String> seenIndexNames = new HashSet<>();
        for (IndexSpec indexSpec : tableSpec.getIndexes()) {
            validateIdentifier("index", indexSpec.getName(), issues);
            if (!seenIndexNames.add(indexSpec.getName())) {
                issues.add(new SchemaDefinitionIssue(
                        SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION,
                        "index `" + indexSpec.getName() + "`",
                        "Duplicate index name detected in same table."
                ));
            }
            validateIndexColumns(indexSpec, columnNames, issues);
        }
        return issues;
    }

    private void validateIdentifier(String kind, String identifier,
                                    List<SchemaDefinitionIssue> issues) {
        String subject = kind + " `" + identifier + "`";
        if (StringUtils.isBlank(identifier)) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.ILLEGAL_IDENTIFIER,
                    subject,
                    "Identifier cannot be blank."
            ));
            return;
        }
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.ILLEGAL_IDENTIFIER,
                    subject,
                    "Identifier length exceeds 64 characters."
            ));
        }
        if (identifier.contains("`")) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.ILLEGAL_IDENTIFIER,
                    subject,
                    "Identifier cannot contain backtick (`)."
            ));
        }
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.ILLEGAL_IDENTIFIER,
                    subject,
                    "Identifier must match pattern [A-Za-z_][A-Za-z0-9_]*."
            ));
        }
        if (RESERVED_KEYWORDS.contains(identifier.toUpperCase(Locale.ROOT))) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.RESERVED_KEYWORD,
                    subject,
                    "Identifier conflicts with MySQL reserved keyword."
            ));
        }
    }

    private void validateIndexColumns(IndexSpec indexSpec,
                                      Set<String> validColumns,
                                      List<SchemaDefinitionIssue> issues) {
        if (StringUtils.isBlank(indexSpec.getColumns())) {
            issues.add(new SchemaDefinitionIssue(
                    SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION,
                    "index `" + indexSpec.getName() + "`",
                    "Index column list cannot be empty."
            ));
            return;
        }
        for (String item : indexSpec.getColumns().split(",")) {
            String token = item.trim();
            if (token.isBlank()) {
                issues.add(new SchemaDefinitionIssue(
                        SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION,
                        "index `" + indexSpec.getName() + "`",
                        "Index contains empty column segment."
                ));
                continue;
            }
            String[] split = token.split("\\s+");
            String columnName = split[0];
            if (!validColumns.contains(columnName)) {
                issues.add(new SchemaDefinitionIssue(
                        SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION,
                        "index `" + indexSpec.getName() + "`",
                        "Index refers to undefined column `" + columnName + "`."
                ));
            }
            if (split.length > 1) {
                String order = split[1].toUpperCase(Locale.ROOT);
                if (!"ASC".equals(order) && !"DESC".equals(order)) {
                    issues.add(new SchemaDefinitionIssue(
                            SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION,
                            "index `" + indexSpec.getName() + "`",
                            "Index order must be ASC or DESC."
                    ));
                }
            }
        }
    }
}
