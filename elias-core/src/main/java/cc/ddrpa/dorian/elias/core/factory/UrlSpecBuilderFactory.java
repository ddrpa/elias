package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.ConstantsPool;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsURL;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class UrlSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsURL.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsURL anno = Objects.requireNonNull(field.getAnnotation(IsURL.class));
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        long length = anno.length();
        if (length > ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH) {
            builder.setDataType("text");
        } else {
            builder.setDataType("varchar").setLength(length);
        }
        return applyCharLengthOverride(builder, field);
    }
}
