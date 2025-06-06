package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.enums.SpatialDataType;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsGeo;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GeometrySpecBuilderFactory implements SpecBuilderFactory {

    private static final Map<String, String> CLASS_TYPE_MAP = new HashMap<>();
    private static final Map<SpatialDataType, String> ENUM_TYPE_MAP = new HashMap<>();

    static {
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.Geometry", "geometry");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.Point", "point");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.LineString", "linestring");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.Polygon", "polygon");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.GeometryCollection", "geometrycollection");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.MultiPoint", "multipoint");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.MultiLineString", "multilinestring");
        CLASS_TYPE_MAP.put("org.locationtech.jts.geom.MultiPolygon", "multipolygon");

        ENUM_TYPE_MAP.put(SpatialDataType.GEOMETRY, "geometry");
        ENUM_TYPE_MAP.put(SpatialDataType.POINT, "point");
        ENUM_TYPE_MAP.put(SpatialDataType.LINESTRING, "linestring");
        ENUM_TYPE_MAP.put(SpatialDataType.POLYGON, "polygon");
        ENUM_TYPE_MAP.put(SpatialDataType.GEOMETRYCOLLECTION, "geometrycollection");
        ENUM_TYPE_MAP.put(SpatialDataType.MULTIPOINT, "multipoint");
        ENUM_TYPE_MAP.put(SpatialDataType.MULTILINESTRING, "multilinestring");
        ENUM_TYPE_MAP.put(SpatialDataType.MULTIPOLYGON, "multipolygon");
    }

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.isAnnotationPresent(IsGeo.class)) {
            return true;
        }
        return CLASS_TYPE_MAP.containsKey(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (field.isAnnotationPresent(IsGeo.class)) {
            IsGeo isGeoAnno = Objects.requireNonNull(field.getAnnotation(IsGeo.class));
            builder.setDataType(ENUM_TYPE_MAP.get(isGeoAnno.type()))
                .setSrid(isGeoAnno.srid())
                .setNullable(isGeoAnno.nullable());
        } else {
            // 除非特别指定，为了添加空间索引，地理空间数据类型不允许为 NULL
            // NEED_CHECK
            builder.setDataType(CLASS_TYPE_MAP.get(field.getType().getName()))
                .setNullable(false);
        }
        return builder;
    }
}