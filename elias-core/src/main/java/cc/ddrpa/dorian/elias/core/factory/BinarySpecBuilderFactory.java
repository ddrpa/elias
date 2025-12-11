package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsHash;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUID;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;

public class BinarySpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsUUID.class) || field.isAnnotationPresent(IsHash.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        long length;
        
        if (field.isAnnotationPresent(IsHash.class)) {
            // IsHash 注解，根据 HashType 设置长度
            IsHash isHash = field.getAnnotation(IsHash.class);
            length = isHash.value().getLength();
        } else {
            // IsUUID 注解，固定设置为 BINARY(16)
            length = 16L;
        }
        
        return SpecBuilderFactory.super.builder(field)
                .setDataType("binary")
                .setLength(length);
    }
}