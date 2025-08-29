package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;

/**
 * Factory for BLOB column specifications from byte arrays and Blob types.
 */
public class BlobSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.getType().isArray()) {
            String simpleFieldType = field.getType().getSimpleName();
            if (simpleFieldType.equalsIgnoreCase("byte[]")
                || simpleFieldType.equalsIgnoreCase("java.lang.Byte[]")) {
                return true;
            }
        }
        return fieldTypeName.equalsIgnoreCase("java.sql.Blob");
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        return SpecBuilderFactory.super.builder(field)
            .setDataType("blob")
            .setLength(64000L);
    }
}