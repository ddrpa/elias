package cc.ddrpa.dorian.elias.generator.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DiffResult {

    private final List<ExportChange> changes = new ArrayList<>();

    public void add(ExportChange change) {
        changes.add(change);
    }

    public List<ExportChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * True when at least one change has non-blank forward SQL worth writing to a changeset file.
     */
    public boolean hasExportableSql() {
        for (ExportChange change : changes) {
            if (change.getSql() != null && !change.getSql().isBlank()) {
                return true;
            }
        }
        return false;
    }

    public List<ExportChange> getDestructiveChanges() {
        return changes.stream().filter(ExportChange::isDestructive).toList();
    }

    /**
     * Returns a copy without the given change kinds (e.g. exclude DROP_COLUMN / DROP_TABLE).
     */
    public DiffResult withoutKinds(Set<ExportChange.Kind> excludedKinds) {
        if (excludedKinds == null || excludedKinds.isEmpty()) {
            return this;
        }
        DiffResult filtered = new DiffResult();
        for (ExportChange change : changes) {
            if (!excludedKinds.contains(change.getKind())) {
                filtered.add(change);
            }
        }
        return filtered;
    }

    public DiffResult withoutKinds(ExportChange.Kind first, ExportChange.Kind... rest) {
        return withoutKinds(EnumSet.of(first, rest));
    }
}
