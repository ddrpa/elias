package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class IndexReconciliationTest {

    @Test
    void recreatesWhenSameNameDefinitionChanges() {
        IndexProperties actual = new IndexProperties("idx_name", false, List.of("name ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(true).setColumns("name ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(actual));
        Assertions.assertEquals(1, decisions.size());
        Assertions.assertEquals(IndexReconciliation.Kind.RECREATE, decisions.get(0).kind());
    }

    @Test
    void renamesWhenExactMatchHasDifferentName() {
        IndexProperties actual = new IndexProperties("idx_legacy", false, List.of("name ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(actual));
        Assertions.assertEquals(IndexReconciliation.Kind.RENAME, decisions.get(0).kind());
        Assertions.assertEquals("idx_legacy", decisions.get(0).actual().getName());
        Assertions.assertTrue(IndexReconciliation.retainedActualNames(decisions).contains("idx_legacy"));
    }

    @Test
    void coversPrefixWithoutRename() {
        IndexProperties actual = new IndexProperties(
                "idx_name_email", false, List.of("name ASC", "email ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(actual));
        Assertions.assertEquals(IndexReconciliation.Kind.COVERED, decisions.get(0).kind());
        Assertions.assertTrue(IndexReconciliation.retainedActualNames(decisions)
                .contains("idx_name_email"));
    }

    @Test
    void prefersRenameOverCoveringWhenExactMatchExists() {
        IndexProperties exact = new IndexProperties("idx_legacy", false, List.of("name ASC"));
        IndexProperties prefix = new IndexProperties(
                "idx_name_email", false, List.of("name ASC", "email ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(prefix, exact));
        Assertions.assertEquals(IndexReconciliation.Kind.RENAME, decisions.get(0).kind());
        Assertions.assertEquals("idx_legacy", decisions.get(0).actual().getName());
    }

    @Test
    void createsWhenNothingCovers() {
        IndexProperties other = new IndexProperties("idx_email", false, List.of("email ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(other));
        Assertions.assertEquals(IndexReconciliation.Kind.CREATE, decisions.get(0).kind());
        Assertions.assertFalse(IndexReconciliation.retainedActualNames(decisions).contains("idx_email"));
    }

    @Test
    void matchesH2SyntheticUniqueKeyAliasWhenEnabled() {
        IndexProperties actual = new IndexProperties(
                "uk_gdt_account_id_INDEX_2", true, List.of("account_id ASC"));
        IndexSpec expected = new IndexSpec()
                .setName("uk_gdt_account_id")
                .setUnique(true)
                .setColumns("account_id ASC");

        List<IndexReconciliation.Decision> withoutAlias =
                IndexReconciliation.plan(List.of(expected), List.of(actual), false);
        Assertions.assertEquals(IndexReconciliation.Kind.RENAME, withoutAlias.get(0).kind());

        List<IndexReconciliation.Decision> withAlias =
                IndexReconciliation.plan(List.of(expected), List.of(actual), true);
        Assertions.assertEquals(1, withAlias.size());
        Assertions.assertEquals(IndexReconciliation.Kind.MATCH, withAlias.get(0).kind());
        Assertions.assertEquals("uk_gdt_account_id_INDEX_2", withAlias.get(0).actual().getName());
        Assertions.assertTrue(IndexReconciliation.retainedActualNames(withAlias)
                .contains("uk_gdt_account_id_index_2"));
        Assertions.assertTrue(IndexReconciliation.retainedActualNames(withAlias)
                .contains("uk_gdt_account_id"));
    }

    @Test
    void recreatesH2AliasWhenDefinitionDiffers() {
        IndexProperties actual = new IndexProperties(
                "uk_foo_INDEX_E", false, List.of("account_id ASC"));
        IndexSpec expected = new IndexSpec()
                .setName("uk_foo")
                .setUnique(true)
                .setColumns("account_id ASC");
        List<IndexReconciliation.Decision> decisions =
                IndexReconciliation.plan(List.of(expected), List.of(actual), true);
        Assertions.assertEquals(IndexReconciliation.Kind.RECREATE, decisions.get(0).kind());
        Assertions.assertTrue(IndexReconciliation.retainedActualNames(decisions)
                .contains("uk_foo_index_e"));
    }
}
