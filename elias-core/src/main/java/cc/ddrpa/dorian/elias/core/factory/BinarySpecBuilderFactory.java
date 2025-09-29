package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUID;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;

public class BinarySpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsUUID.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        // 目前只有 IsUUID 会使用这个，固定设置为 BINARY(16)
        return SpecBuilderFactory.super.builder(field)
                .setDataType("binary")
                .setLength(16L);
    }
}