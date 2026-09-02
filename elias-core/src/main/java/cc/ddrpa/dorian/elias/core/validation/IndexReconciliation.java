package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reconciles expected index specs against observed indexes: same-name match/recreate,
 * exact-definition rename, functional covering, then create.
 */
public final class IndexReconciliation {

    private IndexReconciliation() {
    }

    public enum Kind {
        MATCH,
        RECREATE,
        RENAME,
        COVERED,
        CREATE
    }

    public record Decision(Kind kind, IndexSpec expected, IndexProperties actual) {
    }

    public static List<Decision> plan(List<IndexSpec> expectedIndexes,
                                      Collection<IndexProperties> actualIndexes) {
        return plan(expectedIndexes, actualIndexes, false);
    }

    /**
     * @param h2IndexAliases when true, treat H2 {@code name_INDEX_*} metadata names as the
     *                       expected {@code name} for same-name MATCH/RECREATE
     */
    public static List<Decision> plan(List<IndexSpec> expectedIndexes,
                                      Collection<IndexProperties> actualIndexes,
                                      boolean h2IndexAliases) {
        Map<String, IndexProperties> byName = new LinkedHashMap<>();
        for (IndexProperties actual : actualIndexes) {
            byName.put(actual.getName().toLowerCase(Locale.ROOT), actual);
        }
        Set<String> expectedNames = expectedIndexes.stream()
                .map(index -> index.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        List<Decision> decisions = new ArrayList<>();
        List<IndexSpec> unresolved = new ArrayList<>();
        Set<String> claimedActual = new HashSet<>();
        for (IndexSpec expected : expectedIndexes) {
            IndexProperties sameName = findSameNameOrH2Alias(
                    expected.getName(), byName, claimedActual, h2IndexAliases);
            if (sameName != null) {
                claimedActual.add(sameName.getName().toLowerCase(Locale.ROOT));
                if (sameName.validate(expected).isEmpty()) {
                    decisions.add(new Decision(Kind.MATCH, expected, sameName));
                } else {
                    decisions.add(new Decision(Kind.RECREATE, expected, sameName));
                }
            } else {
                unresolved.add(expected);
            }
        }

        Set<String> claimedForRename = new HashSet<>();
        List<IndexSpec> afterRename = new ArrayList<>();
        for (IndexSpec expected : unresolved) {
            List<IndexProperties> renameCandidates = byName.values().stream()
                    .filter(candidate -> !expectedNames.contains(
                            candidate.getName().toLowerCase(Locale.ROOT)))
                    .filter(candidate -> !claimedActual.contains(
                            candidate.getName().toLowerCase(Locale.ROOT)))
                    .filter(candidate -> !claimedForRename.contains(
                            candidate.getName().toLowerCase(Locale.ROOT)))
                    .toList();
            Optional<IndexProperties> renameSource =
                    IndexCoverage.findExactMatch(renameCandidates, expected);
            if (renameSource.isPresent()) {
                claimedForRename.add(renameSource.get().getName().toLowerCase(Locale.ROOT));
                decisions.add(new Decision(Kind.RENAME, expected, renameSource.get()));
            } else {
                afterRename.add(expected);
            }
        }

        for (IndexSpec expected : afterRename) {
            Optional<IndexProperties> covering = IndexCoverage.findCovering(byName.values(), expected);
            if (covering.isPresent()) {
                decisions.add(new Decision(Kind.COVERED, expected, covering.get()));
            } else {
                decisions.add(new Decision(Kind.CREATE, expected, null));
            }
        }
        return decisions;
    }

    private static IndexProperties findSameNameOrH2Alias(
            String expectedName,
            Map<String, IndexProperties> byName,
            Set<String> claimedActual,
            boolean h2IndexAliases) {
        IndexProperties exact = byName.get(expectedName.toLowerCase(Locale.ROOT));
        if (exact != null && !claimedActual.contains(exact.getName().toLowerCase(Locale.ROOT))) {
            return exact;
        }
        if (!h2IndexAliases) {
            return null;
        }
        for (IndexProperties candidate : byName.values()) {
            String key = candidate.getName().toLowerCase(Locale.ROOT);
            if (claimedActual.contains(key)) {
                continue;
            }
            if (H2IndexNames.isSyntheticAlias(expectedName, candidate.getName())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Actual index names that must not be dropped as extras: expected names,
     * rename sources, and covering sources (PRIMARY is filtered by callers).
     */
    public static Set<String> retainedActualNames(List<Decision> decisions) {
        Set<String> retained = new HashSet<>();
        for (Decision decision : decisions) {
            if (decision.kind() == Kind.MATCH || decision.kind() == Kind.RECREATE) {
                retained.add(decision.expected().getName().toLowerCase(Locale.ROOT));
                if (decision.actual() != null) {
                    retained.add(decision.actual().getName().toLowerCase(Locale.ROOT));
                }
            } else if (decision.kind() == Kind.RENAME || decision.kind() == Kind.COVERED) {
                retained.add(decision.actual().getName().toLowerCase(Locale.ROOT));
            }
        }
        return retained;
    }
}
