package cc.ddrpa.dorian.elias.core.spec;

import java.util.ArrayList;
import java.util.List;

public class TableSpec {

    private String name;
    private List<ColumnSpec> columns = new ArrayList<>(5);
    private List<IndexSpec> indexes = new ArrayList<>(2);

    public String getName() {
        return name;
    }

    public TableSpec setName(String name) {
        this.name = name;
        return this;
    }

    public List<ColumnSpec> getColumns() {
        return columns;
    }

    public TableSpec setColumns(List<ColumnSpec> columns) {
        this.columns = columns;
        return this;
    }

    public List<IndexSpec> getIndexes() {
        return indexes;
    }

    public TableSpec setIndexes(List<IndexSpec> indexes) {
        this.indexes = indexes;
        return this;
    }
}