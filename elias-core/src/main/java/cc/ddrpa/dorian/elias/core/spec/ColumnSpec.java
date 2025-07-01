package cc.ddrpa.dorian.elias.core.spec;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class ColumnSpec {

    private static final List<String> GEOMETRY_TYPES = List.of(
        "geometry", "point", "linestring", "polygon", "multipoint", "multilinestring",
        "multipolygon",
        "geometrycollection"
    );

    /**
     * 列名
     */
    @Setter
    private String name;
    /**
     * SQL 数据类型，例如 varchar
     */
    private String dataType;
    /**
     * 对于字符和 blob 类型，为最大长度，非字符串类型为可视长度
     */
    @Setter
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
    @Setter
    private String columnType = null;

    // 用于空间数据类型
    @Setter
    private int srid;
    private Boolean characterType = false;
    private Boolean textType = false;
    private Boolean blobType = false;
    private Boolean decimalType = false;
    @Setter
    private boolean nullable = true;
    @Setter
    private boolean primaryKey = false;
    @Setter
    private boolean autoIncrement = false;
    @Setter
    private String defaultValue;
    // 不好弄，运行时读不到注释了
    @Setter
    private String comment;

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

    public String getColumnType() {
        if (Objects.isNull(columnType)) {
            return setColumnType();
        }
        return this.columnType;
    }

    public boolean isGeometry() {
        return GEOMETRY_TYPES.contains(this.dataType);
    }
}