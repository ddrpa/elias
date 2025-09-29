package cc.ddrpa.dorian.elias.core.spec;

import java.util.Objects;

public class ColumnSpecBuilder {

    private String name;

    // SQL 数据类型，例如 varchar
    private String dataType;

    // 对于字符和 blob 类型，为最大长度，非字符串类型为可视长度
    private long length = -1L;
    // 对于整数类型，为精度
    private int precision = -1;
    // 对于 decimal 类型，为小数位数
    private int scale = -1;
    // 几何数据类型使用
    private int srid = -1;
    private boolean primaryKey = false;
    private boolean autoIncrement = false;
    private boolean nullable = true;
    private String defaultValue;

    private String comment;

    public ColumnSpecBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ColumnSpecBuilder setDataType(String dataType) {
        this.dataType = dataType;
        return this;
    }

    public ColumnSpecBuilder setLength(long length) {
        this.length = length;
        return this;
    }

    public ColumnSpecBuilder setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    public ColumnSpecBuilder setScale(int scale) {
        this.scale = scale;
        return this;
    }

    public ColumnSpecBuilder setSrid(int srid) {
        this.srid = srid;
        return this;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public ColumnSpecBuilder setPrimaryKey(boolean isAutoIncrement) {
        this.primaryKey = true;
        // 主键不允许为 null
        this.nullable = false;
        this.autoIncrement = isAutoIncrement;
        // 主键不允许有默认值
        this.defaultValue = null;
        return this;
    }

    public ColumnSpecBuilder setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ColumnSpecBuilder setNullable(Boolean nullable) {
        if (isPrimaryKey()) {
            throw new IllegalStateException("Primary key column cannot be nullable");
        }
        this.nullable = nullable;
        return this;
    }

    public ColumnSpecBuilder setDefaultValue(String defaultValue) {
        if (isPrimaryKey()) {
            throw new IllegalStateException("Primary key column cannot have a default value");
        }
        this.defaultValue = defaultValue;
        return this;
    }

    public ColumnSpec build() {
        ColumnSpec spec = new ColumnSpec()
                .setName(this.name);
        spec.setDataType(this.dataType);
        if (this.length > 0) {
            spec.setLength(this.length);
        } else if (this.precision >= 0 && this.scale >= 0) {
            spec.setPrecisionAndScale(this.precision, this.scale);
        } else if (this.srid >= 0) {
            spec.setSrid(this.srid);
        }
        if (this.primaryKey) {
            spec.setPrimaryKey(true)
                    .setAutoIncrement(this.autoIncrement)
                    .setNullable(false);
        } else {
            spec.setNullable(this.nullable);
            if (Objects.nonNull(this.defaultValue)) {
                spec.setDefaultValue(this.defaultValue);
            }
        }
        spec.setComment(this.comment);
        return spec;
    }
}