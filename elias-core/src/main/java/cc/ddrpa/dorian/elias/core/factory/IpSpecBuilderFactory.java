package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsIP;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class IpSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsIP.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsIP anno = Objects.requireNonNull(field.getAnnotation(IsIP.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        switch (anno.version()) {
            case V4 -> builder.setDataType("binary").setLength(4L);
            case V6 -> builder.setDataType("binary").setLength(16L);
            case ALL -> builder.setDataType("varbinary").setLength(16L);
        }
        return builder;
    }
}
