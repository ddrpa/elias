package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsEmail;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class EmailSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsEmail.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsEmail anno = Objects.requireNonNull(field.getAnnotation(IsEmail.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field)
                .setDataType("varchar")
                .setLength(anno.length());
        return applyCharLengthOverride(builder, field);
    }
}
