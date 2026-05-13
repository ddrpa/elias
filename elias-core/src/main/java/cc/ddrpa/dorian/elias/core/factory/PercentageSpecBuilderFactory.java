package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsPercentage;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class PercentageSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsPercentage.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsPercentage anno = Objects.requireNonNull(field.getAnnotation(IsPercentage.class));
        return SpecBuilderFactory.super.builder(field)
                .setDataType("decimal")
                .setPrecision(anno.precision())
                .setScale(anno.scale());
    }
}
