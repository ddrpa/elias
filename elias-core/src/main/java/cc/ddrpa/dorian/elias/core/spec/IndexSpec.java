package cc.ddrpa.dorian.elias.core.spec;

import java.util.Objects;

public class IndexSpec {

    private String name;
    private boolean unique;
    private String columns;

    public String getName() {
        return name;
    }

    public IndexSpec setName(String name) {
        this.name = name;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public IndexSpec setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public String getColumns() {
        return columns;
    }

    public IndexSpec setColumns(String columns) {
        this.columns = columns;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSpec indexSpec = (IndexSpec) o;
        return unique == indexSpec.unique &&
                Objects.equals(name, indexSpec.name) &&
                Objects.equals(columns, indexSpec.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, unique, columns);
    }

    @Override
    public String toString() {
        return "IndexSpec{" +
                "name='" + name + '\'' +
                ", unique=" + unique +
                ", columns='" + columns + '\'' +
                '}';
    }
}
