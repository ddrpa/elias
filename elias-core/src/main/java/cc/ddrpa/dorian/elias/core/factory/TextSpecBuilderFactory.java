package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.types.CharLength;
import cc.ddrpa.dorian.elias.core.annotation.types.UseText;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import cc.ddrpa.dorian.elias.core.ConstantsPool;
import java.lang.reflect.Field;
import java.util.Objects;

public class TextSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.isAnnotationPresent(UseText.class)) {
            return true;
        }
        if (field.isAnnotationPresent(CharLength.class)) {
            return true;
        }
        if (field.getType().isArray()) {
            String simpleFieldType = field.getType().getSimpleName();
            if (simpleFieldType.equalsIgnoreCase("char[]")
                || simpleFieldType.equalsIgnoreCase("java.lang.Character[]")) {
                return true;
            }
        }
        return fieldTypeName.equalsIgnoreCase("java.lang.String") ||
            fieldTypeName.equalsIgnoreCase("java.sql.Clob");
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (field.isAnnotationPresent(UseText.class)) {
            UseText useTextAnno = Objects.requireNonNull(field.getAnnotation(UseText.class));
            if (useTextAnno.estimated() <= ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH) {
                builder.setDataType("varchar")
                    .setLength(useTextAnno.estimated() > 0L
                        ? useTextAnno.estimated()
                        : ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH);
            } else if (useTextAnno.estimated() <= ConstantsPool.TEXT_MAX_CHARACTER_LENGTH) {
                builder.setDataType("text");
            } else if (useTextAnno.estimated() < ConstantsPool.MEDIUMTEXT_MAX_CHARACTER_LENGTH) {
                builder.setDataType("mediumtext");
            } else {
                builder.setDataType("longtext");
            }
        } else if (field.isAnnotationPresent(CharLength.class)) {
            CharLength charLengthAnno = Objects.requireNonNull(
                field.getAnnotation(CharLength.class));
            long estimatedLength = charLengthAnno.length() > 0L ? charLengthAnno.length() : 255L;
            if (estimatedLength > ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH) {
                builder.setDataType("text");
            } else if (charLengthAnno.fixed()) {
                builder.setDataType("char").setLength(charLengthAnno.length());
            } else {
                builder.setDataType("varchar").setLength(charLengthAnno.length());
            }
        } else if (field.getType().isArray()) {
            String simpleFieldType = field.getType().getSimpleName();
            if (simpleFieldType.equalsIgnoreCase("char[]")
                || simpleFieldType.equalsIgnoreCase("java.lang.Character[]")) {
                builder.setDataType("text");
            }
        } else if (field.getType().getName().equalsIgnoreCase("java.sql.Clob")) {
            builder.setDataType("text");
        } else {
            builder.setDataType("varchar").setLength(255L);
        }
        return builder;
    }

}