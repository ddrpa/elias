package cc.ddrpa.dorian.elias.core.annotation.preset;

import cc.ddrpa.dorian.elias.core.annotation.enums.SpatialDataType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MySQL 5.7.5 之后支持的地理空间数据类型，见
 * <a href="https://dev.mysql.com/doc/refman/5.7/en/spatial-type-overview.html">11.4.1 Spatial Data
 * Types</a>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsGeo {

    // 默认使用 WGS84 坐标系，MySQL 5.7 中不能在建表语句中指定
    int srid() default 4326;

    // 是否允许 NULL 值
    boolean nullable() default false;

    // 默认使用 GEOMETRY 类型
    SpatialDataType type() default SpatialDataType.GEOMETRY;
}
