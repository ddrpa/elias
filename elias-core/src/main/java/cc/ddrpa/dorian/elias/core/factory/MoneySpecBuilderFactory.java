package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsMoney;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class MoneySpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsMoney.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsMoney anno = Objects.requireNonNull(field.getAnnotation(IsMoney.class));
        return SpecBuilderFactory.super.builder(field)
                .setDataType("decimal")
                .setPrecision(anno.precision())
                .setScale(anno.scale());
    }
}
