package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class IndexCoverage {

    private IndexCoverage() {
    }

    public static boolean isPrimaryIndex(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            return false;
        }
        String upper = indexName.toUpperCase(Locale.ROOT);
        return "PRIMARY".equals(upper) || upper.startsWith("PRIMARY_KEY");
    }

    /**
     * Exact unique + full column match, excluding PRIMARY. Ties broken by name.
     */
    public static Optional<IndexProperties> findExactMatch(
            Collection<IndexProperties> candidates, IndexSpec expected) {
        return candidates.stream()
                .filter(candidate -> !isPrimaryIndex(candidate.getName()))
                .filter(candidate -> candidate.exactMatch(expected))
                .min(Comparator.comparing(candidate -> candidate.getName().toLowerCase(Locale.ROOT)));
    }

    /**
     * Best covering index: same columns and unique, then unique covering non-unique,
     * then fewer extra columns, then name.
     */
    public static Optional<IndexProperties> findCovering(
            Collection<IndexProperties> candidates, IndexSpec expected) {
        return candidates.stream()
                .filter(candidate -> candidate.covers(expected))
                .min(coveringComparator(expected));
    }

    private static Comparator<IndexProperties> coveringComparator(IndexSpec expected) {
        int expectedLength = IndexProperties.parseColumns(expected.getColumns()).size();
        return Comparator
                .comparingInt((IndexProperties actual) -> coveringRank(actual, expected, expectedLength))
                .thenComparingInt(actual -> actual.getOrderedColumns().size())
                .thenComparing(actual -> actual.getName().toLowerCase(Locale.ROOT));
    }

    private static int coveringRank(IndexProperties actual, IndexSpec expected, int expectedLength) {
        boolean sameLength = actual.getOrderedColumns().size() == expectedLength;
        if (sameLength && actual.isUnique() == expected.isUnique()) {
            return 0;
        }
        if (sameLength) {
            return 1;
        }
        return 2;
    }
}
