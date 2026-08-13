package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.ConstantsPool;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;

public class BlobSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.getType().isArray()) {
            Class<?> componentType = field.getType().getComponentType();
            if (componentType == byte.class || componentType == Byte.class) {
                return true;
            }
        }
        return fieldTypeName.equalsIgnoreCase("java.sql.Blob");
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        return SpecBuilderFactory.super.builder(field)
                .setDataType("blob")
                .setLength(ConstantsPool.BLOB_DEFAULT_LENGTH);
    }
}