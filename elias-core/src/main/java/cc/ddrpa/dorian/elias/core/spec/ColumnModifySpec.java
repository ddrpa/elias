package cc.ddrpa.dorian.elias.core.spec;

import java.util.ArrayList;
import java.util.List;

public class ColumnModifySpec {

    private boolean alterColumnType = false;
    private String newColumnType;
    private boolean alterNullable = false;
    private boolean newNullable;
    private boolean alterDefaultValue = false;
    private String newDefaultValue;

    private boolean autoFixEnabled = true;
    private List<String> warnings = new ArrayList<>(0);

    /**
     * 变更是否可以自动执行
     *
     * @return
     */
    public boolean isAutoFixEnabled() {
        return autoFixEnabled;
    }

    /**
     * 添加属性变更警告，同时标记差异不可自动修复
     *
     * @param warning
     */
    public void warn(String warning) {
        warnings.add(warning);
        this.autoFixEnabled = false;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isAlterColumnType() {
        return alterColumnType;
    }

    public ColumnModifySpec setAlterColumnType(boolean alterColumnType) {
        this.alterColumnType = alterColumnType;
        return this;
    }

    public String getNewColumnType() {
        return newColumnType;
    }

    public ColumnModifySpec setNewColumnType(String newColumnType) {
        this.newColumnType = newColumnType;
        return this;
    }

    public boolean isAlterNullable() {
        return alterNullable;
    }

    public ColumnModifySpec setAlterNullable(boolean alterNullable) {
        this.alterNullable = alterNullable;
        return this;
    }

    public boolean isNewNullable() {
        return newNullable;
    }

    public ColumnModifySpec setNewNullable(boolean newNullable) {
        this.newNullable = newNullable;
        return this;
    }

    public boolean isAlterDefaultValue() {
        return alterDefaultValue;
    }

    public ColumnModifySpec setAlterDefaultValue(boolean alterDefaultValue) {
        this.alterDefaultValue = alterDefaultValue;
        return this;
    }

    public String getNewDefaultValue() {
        return newDefaultValue;
    }

    public ColumnModifySpec setNewDefaultValue(String newDefaultValue) {
        this.newDefaultValue = newDefaultValue;
        return this;
    }
}