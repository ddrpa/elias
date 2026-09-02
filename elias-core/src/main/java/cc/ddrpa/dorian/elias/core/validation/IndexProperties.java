package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.IndexSpecMismatch;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class IndexProperties {

    private final String name;
    private final boolean unique;
    private final List<String> orderedColumns;

    public IndexProperties(String name, boolean unique, List<String> orderedColumns) {
        this.name = name;
        this.unique = unique;
        this.orderedColumns = orderedColumns;
    }

    public String getName() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }

    public List<String> getOrderedColumns() {
        return orderedColumns;
    }

    public IndexSpec toIndexSpec() {
        return new IndexSpec()
                .setName(name)
                .setUnique(unique)
                .setColumns(String.join(", ", orderedColumns));
    }

    public Optional<IndexSpecMismatch> validate(IndexSpec indexSpec) {
        List<String> expectedColumns = parseColumns(indexSpec.getColumns());
        boolean uniqueMismatch = unique != indexSpec.isUnique();
        boolean columnsMismatch = !columnListsEqual(expectedColumns, orderedColumns);
        if (!uniqueMismatch && !columnsMismatch) {
            return Optional.empty();
        }
        IndexSpecMismatch mismatch = new IndexSpecMismatch();
        if (uniqueMismatch) {
            mismatch.addUniqueMismatch(indexSpec.isUnique(), unique);
        }
        if (columnsMismatch) {
            mismatch.addColumnsMismatch(expectedColumns, orderedColumns);
        }
        return Optional.of(mismatch);
    }

    /**
     * Same uniqueness and the full column list (names and ASC/DESC).
     */
    public boolean exactMatch(IndexSpec expected) {
        List<String> expectedColumns = parseColumns(expected.getColumns());
        return unique == expected.isUnique()
                && columnListsEqual(expectedColumns, orderedColumns);
    }

    /**
     * Whether this index can serve the expected one: exact match, unique covering
     * non-unique on the same columns, or a longer index whose leftmost prefix matches.
     * A unique expected index is only covered by a unique index on the same columns.
     */
    public boolean covers(IndexSpec expected) {
        List<String> expectedColumns = parseColumns(expected.getColumns());
        if (expectedColumns.isEmpty() || orderedColumns.size() < expectedColumns.size()) {
            return false;
        }
        List<String> prefix = orderedColumns.subList(0, expectedColumns.size());
        if (!columnListsEqual(expectedColumns, prefix)) {
            return false;
        }
        if (expected.isUnique()) {
            return unique && orderedColumns.size() == expectedColumns.size();
        }
        return true;
    }

    public static List<String> parseColumns(String columns) {
        if (columns == null || columns.isBlank()) {
            return List.of();
        }
        return List.of(columns.split(","))
                .stream()
                .map(item -> item.trim())
                .filter(item -> !item.isBlank())
                .map(item -> {
                    String[] split = item.split("\\s+");
                    String columnName = split[0];
                    String order = split.length > 1 ? split[1].toUpperCase(Locale.ROOT) : "ASC";
                    return columnName + " " + order;
                })
                .toList();
    }

    private static boolean columnListsEqual(List<String> expected, List<String> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!expected.get(i).equalsIgnoreCase(actual.get(i))) {
                return false;
            }
        }
        return true;
    }
}
