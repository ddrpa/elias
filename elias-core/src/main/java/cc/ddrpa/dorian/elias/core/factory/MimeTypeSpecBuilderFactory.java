package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsMimeType;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class MimeTypeSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsMimeType.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsMimeType anno = Objects.requireNonNull(field.getAnnotation(IsMimeType.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field)
                .setDataType("varchar")
                .setLength(anno.length());
        return applyCharLengthOverride(builder, field);
    }
}
