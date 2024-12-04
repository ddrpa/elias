package cc.ddrpa.dorian.elias.core.spec;

import java.util.Objects;

public class ColumnSpec {

    /**
     * 列名
     */
    private String name;
    /**
     * SQL 数据类型，例如 varchar
     */
    private String dataType;
    /**
     * 对于字符和 blob 类型，为最大长度，非字符串类型为可视长度
     */
    private Long length;
    /**
     * 对于整数类型，为精度
     */
    private Integer precision;
    /**
     * 对于 decimal 类型，为小数位数
     */
    private Integer scale;
    /**
     * 例如 varchar(255)，有时候还会包含 unsigned 等信息
     */
    private String columnType = null;
    private Boolean characterType = false;
    private Boolean textType = false;
    private Boolean blobType = false;
    private Boolean decimalType = false;
    private Boolean nullable = true;
    private Boolean primaryKey = false;
    private Boolean autoIncrement = false;
    private String defaultValue;

    public String getName() {
        return name;
    }

    public ColumnSpec setName(String name) {
        this.name = name;
        return this;
    }

    public ColumnSpec setDataType(String dataType) {
        this.characterType = dataType.endsWith("char");
        this.textType = dataType.endsWith("text");
        this.blobType = dataType.equals("blob");
        this.decimalType = dataType.equals("decimal");
        this.dataType = dataType;
        return this;
    }

    public ColumnSpec setLength(Long length) {
        this.length = length;
        return this;
    }

    public ColumnSpec setPrecisionAndScale(Integer precision, Integer scale) {
        this.precision = precision;
        this.scale = scale;
        return this;
    }

    private String setColumnType() {
        if (this.decimalType) {
            this.columnType = String.format("decimal(%d, %d)", precision, scale);
        } else {
            if (Objects.isNull(this.length)) {
                this.columnType = this.dataType;
            } else {
                this.columnType = String.format("%s(%d)", this.dataType, this.length);
            }
        }
        return this.columnType;
    }

    public String getDataType() {
        return dataType;
    }

    public Long getLength() {
        return length;
    }

    public String getColumnType() {
        if (Objects.isNull(columnType)) {
            return setColumnType();
        }
        return this.columnType;
    }

    public Boolean isNullable() {
        return nullable;
    }

    public ColumnSpec setNullable(Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public Boolean isPrimaryKey() {
        return primaryKey;
    }

    public ColumnSpec setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public Boolean isAutoIncrement() {
        return autoIncrement;
    }

    public ColumnSpec setAutoIncrement(Boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ColumnSpec setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}