package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;
import java.util.List;

public class IntegerSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> ACCEPTED_BIG_INTEGER_TYPES = List.of(
        "long",
        "java.lang.Long",
        "java.math.BigInteger"
    );
    private static final List<String> ACCEPTED_INTEGER_TYPES = List.of(
        "int",
        "java.lang.Integer"
    );
    private static final List<String> ACCEPTED_SMALL_INTEGER_TYPES = List.of(
        "short",
        "java.lang.Short",
        "byte",
        "java.lang.Byte"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return ACCEPTED_INTEGER_TYPES.contains(fieldTypeName)
            || ACCEPTED_BIG_INTEGER_TYPES.contains(fieldTypeName)
            || ACCEPTED_SMALL_INTEGER_TYPES.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        String fieldType = field.getType().getName();
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (ACCEPTED_BIG_INTEGER_TYPES.contains(fieldType)) {
            return builder.setDataType("bigint").setLength(20L);
        } else if (ACCEPTED_SMALL_INTEGER_TYPES.contains(fieldType)) {
            // 对于 short 和 byte 类型，使用 smallint
            return builder.setDataType("smallint");
        }
        return builder.setDataType("int");
    }
}