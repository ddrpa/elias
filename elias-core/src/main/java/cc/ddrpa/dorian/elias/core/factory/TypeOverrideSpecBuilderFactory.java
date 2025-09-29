package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.TypeOverride;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class TypeOverrideSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(TypeOverride.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        TypeOverride typeOverrideAnno = Objects.requireNonNull(
                field.getAnnotation(TypeOverride.class));
        String dataType = typeOverrideAnno.type().toLowerCase();
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field).setDataType(dataType);
        long length = typeOverrideAnno.length();
        if (length > 0) {
            builder.setLength(length);
        }
        return builder;
    }
}