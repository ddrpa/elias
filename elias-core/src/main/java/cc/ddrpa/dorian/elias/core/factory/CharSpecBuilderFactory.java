package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUIDAsStr;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;

public class CharSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.isAnnotationPresent(IsUUIDAsStr.class)) {
            return true;
        }
        return fieldTypeName.equalsIgnoreCase("char")
            || fieldTypeName.equalsIgnoreCase("java.lang.Character");
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        builder.setDataType("char");
        if (field.isAnnotationPresent(IsUUIDAsStr.class)) {
            // 如果字段有 IsUUIDAsStr 注解，设置为 CHAR(36)
            builder.setLength(36L);
        } else {
            // 默认设置为 CHAR(1)
            builder.setLength(1L);
        }
        return builder;
    }
}