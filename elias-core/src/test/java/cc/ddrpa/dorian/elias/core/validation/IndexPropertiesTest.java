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
}
