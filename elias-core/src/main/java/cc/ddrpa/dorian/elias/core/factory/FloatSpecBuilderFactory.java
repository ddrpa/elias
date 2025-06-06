package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;
import java.util.List;

public class FloatSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> ACCEPTED_FLOAT_TYPES = List.of(
        "float",
        "java.lang.Float"
    );
    private static final List<String> ACCEPTED_DOUBLE_TYPES = List.of(
        "double",
        "java.lang.Double"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return ACCEPTED_FLOAT_TYPES.contains(fieldTypeName)
            || ACCEPTED_DOUBLE_TYPES.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        String fieldType = field.getType().getName();
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (ACCEPTED_DOUBLE_TYPES.contains(fieldType)) {
            return builder.setDataType("double");
        }
        return builder.setDataType("float");
    }
}