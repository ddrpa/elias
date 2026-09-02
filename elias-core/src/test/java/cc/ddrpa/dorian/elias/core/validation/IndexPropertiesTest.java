package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class IndexPropertiesTest {

    @Test
    void shouldTreatColumnNamesCaseInsensitively() {
        IndexProperties actual = new IndexProperties(
                "idx_username", false, List.of("USERNAME ASC"));
        Optional<?> mismatch = actual.validate(new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username"));
        Assertions.assertTrue(mismatch.isEmpty());
    }

    @Test
    void shouldDetectUniqueAndColumnMismatch() {
        IndexProperties actual = new IndexProperties(
                "idx_username", false, List.of("username DESC"));
        var mismatch = actual.validate(new IndexSpec()
                .setName("idx_username")
                .setUnique(true)
                .setColumns("username ASC"));
        Assertions.assertTrue(mismatch.isPresent());
    }

    @Test
    void toIndexSpecPreservesDefinition() {
        IndexSpec spec = new IndexProperties(
                "uk_email", true, List.of("email ASC", "tenant_id DESC")).toIndexSpec();
        Assertions.assertEquals("uk_email", spec.getName());
        Assertions.assertTrue(spec.isUnique());
        Assertions.assertEquals("email ASC, tenant_id DESC", spec.getColumns());
    }

    @Test
    void exactMatchIgnoresIndexName() {
        IndexProperties actual = new IndexProperties(
                "idx_legacy", false, List.of("username ASC"));
        Assertions.assertTrue(actual.exactMatch(new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username")));
        Assertions.assertFalse(actual.exactMatch(new IndexSpec()
                .setName("idx_username")
                .setUnique(true)
                .setColumns("username")));
    }

    @Test
    void uniqueIndexCoversNonUniqueOnSameColumns() {
        IndexProperties unique = new IndexProperties(
                "uk_username", true, List.of("username ASC"));
        IndexSpec nonUnique = new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username ASC");
        Assertions.assertTrue(unique.covers(nonUnique));
        Assertions.assertFalse(unique.exactMatch(nonUnique));
    }

    @Test
    void longerIndexCoversLeftmostPrefix() {
        IndexProperties composite = new IndexProperties(
                "idx_user_email", false, List.of("username ASC", "email ASC"));
        Assertions.assertTrue(composite.covers(new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username ASC")));
        Assertions.assertFalse(composite.exactMatch(new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username ASC")));
    }

    @Test
    void uniqueExpectedIsNotCoveredByLongerUniqueIndex() {
        IndexProperties compositeUnique = new IndexProperties(
                "uk_user_email", true, List.of("username ASC", "email ASC"));
        Assertions.assertFalse(compositeUnique.covers(new IndexSpec()
                .setName("uk_username")
                .setUnique(true)
                .setColumns("username ASC")));
    }

    @Test
    void differentSortOrderDoesNotCover() {
        IndexProperties actual = new IndexProperties(
                "idx_username", false, List.of("username DESC"));
        Assertions.assertFalse(actual.covers(new IndexSpec()
                .setName("idx_username")
                .setUnique(false)
                .setColumns("username ASC")));
    }
}
