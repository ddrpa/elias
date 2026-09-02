package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class IndexCoverageTest {

    @Test
    void findExactMatchPrefersLexicographicNameAndSkipsPrimary() {
        IndexProperties primary = new IndexProperties("PRIMARY", true, List.of("id ASC"));
        IndexProperties zebra = new IndexProperties("idx_zebra", false, List.of("name ASC"));
        IndexProperties apple = new IndexProperties("idx_apple", false, List.of("name ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");

        IndexProperties chosen = IndexCoverage.findExactMatch(List.of(primary, zebra, apple), expected)
                .orElseThrow();
        Assertions.assertEquals("idx_apple", chosen.getName());
        Assertions.assertTrue(IndexCoverage.findExactMatch(List.of(primary),
                new IndexSpec().setName("uk_id").setUnique(true).setColumns("id ASC")).isEmpty());
    }

    @Test
    void findCoveringPrefersExactThenUniqueThenShorterPrefix() {
        IndexProperties prefixLong = new IndexProperties(
                "idx_name_email_city", false, List.of("name ASC", "email ASC", "city ASC"));
        IndexProperties prefix = new IndexProperties(
                "idx_name_email", false, List.of("name ASC", "email ASC"));
        IndexProperties uniqueSame = new IndexProperties("uk_name", true, List.of("name ASC"));
        IndexSpec expected = new IndexSpec().setName("idx_name").setUnique(false).setColumns("name ASC");

        IndexProperties uniqueChosen = IndexCoverage.findCovering(
                List.of(prefixLong, prefix, uniqueSame), expected).orElseThrow();
        Assertions.assertEquals("uk_name", uniqueChosen.getName());

        IndexProperties shorterPrefix = IndexCoverage.findCovering(
                List.of(prefixLong, prefix), expected).orElseThrow();
        Assertions.assertEquals("idx_name_email", shorterPrefix.getName());
    }

    @Test
    void isPrimaryIndexRecognizesMysqlAndH2Names() {
        Assertions.assertTrue(IndexCoverage.isPrimaryIndex("PRIMARY"));
        Assertions.assertTrue(IndexCoverage.isPrimaryIndex("PRIMARY_KEY_5"));
        Assertions.assertFalse(IndexCoverage.isPrimaryIndex("idx_primary_flag"));
    }
}
