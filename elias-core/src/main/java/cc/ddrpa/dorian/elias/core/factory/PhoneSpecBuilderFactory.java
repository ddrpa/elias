package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsPhone;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class PhoneSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsPhone.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsPhone anno = Objects.requireNonNull(field.getAnnotation(IsPhone.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field)
                .setDataType("varchar")
                .setLength(anno.length());
        return applyCharLengthOverride(builder, field);
    }
}
