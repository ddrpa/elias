package cc.ddrpa.dorian.elias.core.spec;

public class ColumnSpec {

    private String name;
    /**
     * 例如 varchar
     */
    private String dataType;
    /**
     * 字符和 blob 类型为最大长度，非字符串类型为可视长度
     */
    private Long length;
    /**
     * 例如 varchar(255)，有时候还会包含 unsigned 等信息
     */
    private String columnType;
    private Boolean characterType = false;
    private Boolean textType = false;
    private Boolean blobType = false;
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

    /**
     * 设置列类型
     *
     * @param type   数据类型
     * @param length 字符长度或可视长度
     * @return
     */
    public ColumnSpec setColumnType(String type, Long length) {
        if (length != null) {
            this.columnType = String.format("%s(%d)", type, length);
        } else {
            this.columnType = type;
        }
        this.characterType = type.endsWith("char");
        this.textType = type.endsWith("text");
        this.blobType = type.equals("blob");
        this.dataType = type;
        this.length = length;
        return this;
    }

    public String getDataType() {
        return dataType;
    }

    public Long getLength() {
        return length;
    }

    public String getColumnType() {
        return columnType;
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