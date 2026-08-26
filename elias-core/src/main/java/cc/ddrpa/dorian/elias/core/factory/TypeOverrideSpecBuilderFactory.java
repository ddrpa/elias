package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.TypeOverride;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import cc.ddrpa.dorian.elias.core.spec.ColumnTypeParser;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Objects;

public class TypeOverrideSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(TypeOverride.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        TypeOverride typeOverrideAnno = Objects.requireNonNull(
                field.getAnnotation(TypeOverride.class));
        String rawType = typeOverrideAnno.type().trim().toLowerCase(Locale.ROOT);
        ColumnTypeParser.Parsed parsed = ColumnTypeParser.parse(rawType);
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field)
                .setDataType(parsed.dataType());

        if (parsed.precision().isPresent() && parsed.scale().isPresent()) {
            builder.setPrecision(parsed.precision().get())
                    .setScale(parsed.scale().get());
        } else {
            long length = typeOverrideAnno.length();
            if (length > 0) {
                builder.setLength(length);
            } else {
                parsed.length().ifPresent(builder::setLength);
            }
        }

        // 保留 unsigned / zerofill 等修饰，或 "bigint unsigned" 这类非纯 dataType 写法
        if (parsed.preserveColumnType()) {
            builder.setColumnType(rawType);
        }
        return builder;
    }
}
