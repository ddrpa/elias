package cc.ddrpa.dorian.elias.core.spec;

public class IndexSpec {

    private String name;
    private boolean unique;
    private String columnList;

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

    public String getColumnList() {
        return columnList;
    }

    public IndexSpec setColumnList(String columnList) {
        this.columnList = columnList;
        return this;
    }
}
