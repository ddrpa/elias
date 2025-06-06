package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;
import java.util.List;

public class BooleanSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> ACCEPTED_BOOLEAN_TYPES = List.of(
        "boolean",
        "java.lang.Boolean"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return ACCEPTED_BOOLEAN_TYPES.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        return SpecBuilderFactory.super.builder(field)
            .setDataType("tinyint")
            .setLength(1L);
    }
}