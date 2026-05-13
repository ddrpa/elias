package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsMacAddress;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class MacAddressSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsMacAddress.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsMacAddress anno = Objects.requireNonNull(field.getAnnotation(IsMacAddress.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (anno.asString()) {
            builder.setDataType("char").setLength(17L);
        } else {
            builder.setDataType("binary").setLength(6L);
        }
        return builder;
    }
}
