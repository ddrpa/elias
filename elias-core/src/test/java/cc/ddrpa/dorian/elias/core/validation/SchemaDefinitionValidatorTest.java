package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SchemaDefinitionValidatorTest {

    private final SchemaDefinitionValidator validator = new SchemaDefinitionValidator();

    @Test
    void shouldDetectReservedKeywordAndIllegalIdentifier() {
        TableSpec tableSpec = new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(
                        new ColumnSpec().setName("order").setDataType("varchar").setLength(255L),
                        new ColumnSpec().setName("user-name").setDataType("varchar").setLength(255L)
                ))
                .setIndexes(List.of(new IndexSpec().setName("idx_user").setColumns("order ASC")));

        List<SchemaDefinitionIssue> issues = validator.validate(tableSpec);

        Assertions.assertTrue(issues.stream().anyMatch(issue ->
                issue.getType() == SchemaDefinitionIssue.Type.RESERVED_KEYWORD
                        && issue.getSubject().contains("column `order`")));
        Assertions.assertTrue(issues.stream().anyMatch(issue ->
                issue.getType() == SchemaDefinitionIssue.Type.ILLEGAL_IDENTIFIER
                        && issue.getSubject().contains("column `user-name`")));
    }

    @Test
    void shouldDetectInvalidIndexDefinition() {
        TableSpec tableSpec = new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(new ColumnSpec().setName("username").setDataType("varchar")
                        .setLength(255L)))
                .setIndexes(List.of(
                        new IndexSpec().setName("idx_username").setColumns(""),
                        new IndexSpec().setName("idx_username").setColumns("username FOO")
                ));

        List<SchemaDefinitionIssue> issues = validator.validate(tableSpec);

        Assertions.assertTrue(issues.stream().anyMatch(issue ->
                issue.getType() == SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION
                        && issue.getDetail().contains("cannot be empty")));
        Assertions.assertTrue(issues.stream().anyMatch(issue ->
                issue.getType() == SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION
                        && issue.getDetail().contains("Duplicate index name")));
        Assertions.assertTrue(issues.stream().anyMatch(issue ->
                issue.getType() == SchemaDefinitionIssue.Type.INVALID_INDEX_DEFINITION
                        && issue.getDetail().contains("ASC or DESC")));
    }
}
