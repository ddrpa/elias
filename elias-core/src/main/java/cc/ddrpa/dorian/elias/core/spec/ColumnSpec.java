package cc.ddrpa.dorian.elias.core.spec;

import java.util.List;
import java.util.Objects;

/**
 * Complete specification for a database table column including type, constraints, and metadata.
 * 
 * <p>This class captures all properties needed to define a database column, from basic information
 * like name and data type to advanced features like spatial data support and geometric types.
 * 
 * <p>Instances are created by {@link cc.ddrpa.dorian.elias.core.factory.SpecBuilderFactory} 
 * implementations during Java field processing.
 */
public class ColumnSpec {

    private static final List<String> GEOMETRY_TYPES = List.of(
        "geometry", "point", "linestring", "polygon", "multipoint", "multilinestring",
        "multipolygon",
        "geometrycollection"
    );

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

    // 用于空间数据类型
    private int srid;
    private Boolean characterType = false;
    private Boolean textType = false;
    private Boolean blobType = false;
    private Boolean decimalType = false;
    private boolean nullable = true;
    private boolean primaryKey = false;
    private boolean autoIncrement = false;
    private String defaultValue;
    // 不好弄，运行时读不到注释了
    private String comment;

    public String getName() {
        return name;
    }

    public ColumnSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getDataType() {
        return dataType;
    }

    public Long getLength() {
        return length;
    }

    public ColumnSpec setLength(Long length) {
        this.length = length;
        return this;
    }

    public Integer getPrecision() {
        return precision;
    }

    public Integer getScale() {
        return scale;
    }

    public String getColumnType() {
        if (Objects.isNull(columnType)) {
            return setColumnType();
        }
        return this.columnType;
    }

    public ColumnSpec setColumnType(String columnType) {
        this.columnType = columnType;
        return this;
    }

    public int getSrid() {
        return srid;
    }

    public ColumnSpec setSrid(int srid) {
        this.srid = srid;
        return this;
    }

    public Boolean getCharacterType() {
        return characterType;
    }

    public Boolean getTextType() {
        return textType;
    }

    public Boolean getBlobType() {
        return blobType;
    }

    public Boolean getDecimalType() {
        return decimalType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public ColumnSpec setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public ColumnSpec setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public ColumnSpec setAutoIncrement(boolean autoIncrement) {
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

    public String getComment() {
        return comment;
    }

    public ColumnSpec setComment(String comment) {
        this.comment = comment;
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

    public ColumnSpec setDataType(String dataType) {
        this.characterType = dataType.endsWith("char");
        this.textType = dataType.endsWith("text");
        this.blobType = dataType.equals("blob");
        this.decimalType = dataType.equals("decimal");
        this.dataType = dataType;
        return this;
    }

    public boolean isGeometry() {
        return GEOMETRY_TYPES.contains(this.dataType);
    }
}