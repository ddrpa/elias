package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.ConstantsPool;
import cc.ddrpa.dorian.elias.core.annotation.types.Decimal;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class BigDecimalSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> ACCEPTED_TYPES = List.of(
            "java.lang.Number",
            "java.math.BigDecimal"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        if (field.isAnnotationPresent(Decimal.class)) {
            return true;
        }
        return ACCEPTED_TYPES.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        int precision = ConstantsPool.BIG_DECIMAL_DEFAULT_PRECISION;
        int scale = ConstantsPool.BIG_DECIMAL_DEFAULT_SCALE;
        if (field.isAnnotationPresent(Decimal.class)) {
            Decimal decimalAnno = Objects.requireNonNull(field.getAnnotation(Decimal.class));
            precision = decimalAnno.precision();
            scale = decimalAnno.scale();
        }
        return SpecBuilderFactory.super.builder(field)
                .setDataType("decimal")
                .setPrecision(precision)
                .setScale(scale);
    }
}