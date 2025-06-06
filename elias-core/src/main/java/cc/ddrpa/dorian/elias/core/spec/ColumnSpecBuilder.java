package cc.ddrpa.dorian.elias.core.spec;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class ColumnSpecBuilder {

    @Setter
    private String name;

    // SQL 数据类型，例如 varchar
    @Setter
    private String dataType;

    // 对于字符和 blob 类型，为最大长度，非字符串类型为可视长度
    @Setter
    private long length = -1L;
    // 对于整数类型，为精度
    @Setter
    private int precision = -1;
    // 对于 decimal 类型，为小数位数
    @Setter
    private int scale = -1;
    // 几何数据类型使用
    @Setter
    private int srid = -1;
    @Getter
    private boolean primaryKey = false;
    private boolean autoIncrement = false;
    private boolean nullable = true;
    private String defaultValue;

    @Setter
    private String comment;

    public ColumnSpecBuilder setPrimaryKey(boolean isAutoIncrement) {
        this.primaryKey = true;
        // 主键不允许为 null
        this.nullable = false;
        this.autoIncrement = isAutoIncrement;
        // 主键不允许有默认值
        this.defaultValue = null;
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