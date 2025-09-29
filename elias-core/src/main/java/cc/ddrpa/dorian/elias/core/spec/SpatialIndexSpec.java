package cc.ddrpa.dorian.elias.core.spec;

import java.util.Objects;

public class SpatialIndexSpec {

    private String name;
    private String columns;

    public String getName() {
        return name;
    }

    public SpatialIndexSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getColumns() {
        return columns;
    }

    public SpatialIndexSpec setColumns(String columns) {
        this.columns = columns;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpatialIndexSpec that = (SpatialIndexSpec) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns);
    }

    @Override
    public String toString() {
        return "SpatialIndexSpec{" +
                "name='" + name + '\'' +
                ", columns='" + columns + '\'' +
                '}';
    }
}