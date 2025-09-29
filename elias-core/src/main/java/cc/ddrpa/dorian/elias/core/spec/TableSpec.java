package cc.ddrpa.dorian.elias.core.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableSpec {

    private String name;
    private List<ColumnSpec> columns = new ArrayList<>();
    private List<IndexSpec> indexes = new ArrayList<>();
    private List<SpatialIndexSpec> spatialIndexSpecs = new ArrayList<>();

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

    public List<SpatialIndexSpec> getSpatialIndexSpecs() {
        return spatialIndexSpecs;
    }

    public TableSpec setSpatialIndexSpecs(List<SpatialIndexSpec> spatialIndexSpecs) {
        this.spatialIndexSpecs = spatialIndexSpecs;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableSpec tableSpec = (TableSpec) o;
        return Objects.equals(name, tableSpec.name) &&
                Objects.equals(columns, tableSpec.columns) &&
                Objects.equals(indexes, tableSpec.indexes) &&
                Objects.equals(spatialIndexSpecs, tableSpec.spatialIndexSpecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, indexes, spatialIndexSpecs);
    }

    @Override
    public String toString() {
        return "TableSpec{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                ", indexes=" + indexes +
                ", spatialIndexSpecs=" + spatialIndexSpecs +
                '}';
    }
}