package cc.ddrpa.dorian.elias.core.spec;

import java.util.ArrayList;
import java.util.List;

public class ColumnModifySpec {

    // ========== 完整列信息 ==========
    /**
     * 列类型，例如 varchar(255)
     */
    private String columnType;
    /**
     * 是否可空
     */
    private boolean nullable;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 列注释（写入 DDL 的最终值；可能来自 Spec 补写或保留库侧）
     */
    private String comment;

    // ========== 修改标记 ==========
    /**
     * 需要修改列类型
     */
    private boolean alterColumnType = false;
    /**
     * 需要修改是否可空
     */
    private boolean alterNullable = false;
    /**
     * 需要修改默认值
     */
    private boolean alterDefaultValue = false;
    /**
     * 需要补写 comment（库侧原为空）
     */
    private boolean alterComment = false;

    private boolean autoFixEnabled = true;
    private final List<String> warnings = new ArrayList<>(0);

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
        addWarning(warning);
        this.autoFixEnabled = false;
    }

    /**
     * 添加警告信息，但不禁用自动修复
     *
     * @param warning 警告信息
     */
    public void addWarning(String warning) {
        warnings.add(warning);
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

    public String getColumnType() {
        return columnType;
    }

    public ColumnModifySpec setColumnType(String columnType) {
        this.columnType = columnType;
        return this;
    }

    public boolean isAlterNullable() {
        return alterNullable;
    }

    public ColumnModifySpec setAlterNullable(boolean alterNullable) {
        this.alterNullable = alterNullable;
        return this;
    }

    public boolean isNullable() {
        return nullable;
    }

    public ColumnModifySpec setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public boolean isAlterDefaultValue() {
        return alterDefaultValue;
    }

    public ColumnModifySpec setAlterDefaultValue(boolean alterDefaultValue) {
        this.alterDefaultValue = alterDefaultValue;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ColumnModifySpec setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ColumnModifySpec setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * DDL 用的已转义 comment；空白时返回 null。
     */
    public String getSqlComment() {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.replace("'", "''");
    }

    public boolean isAlterComment() {
        return alterComment;
    }

    public ColumnModifySpec setAlterComment(boolean alterComment) {
        this.alterComment = alterComment;
        return this;
    }
}