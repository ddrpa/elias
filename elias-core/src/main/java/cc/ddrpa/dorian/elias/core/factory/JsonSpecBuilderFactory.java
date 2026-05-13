package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.DefaultValue;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsJSON;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * 将 {@link IsJSON} 注解修饰的字段映射为 MySQL {@code JSON} 类型。
 * <p>
 * 注意：MySQL 5.7 / 8.0.13 之前的版本不允许 JSON 列声明 DEFAULT 值，
 * 因此 {@link IsJSON#emptyAs()} 仅作为元数据保留，由应用层在写入时填充；
 * 若字段同时声明 {@link DefaultValue}，DDL 仍会跳过默认值并记录告警。
 */
public class JsonSpecBuilderFactory implements SpecBuilderFactory {

    private static final Logger log = LoggerFactory.getLogger(JsonSpecBuilderFactory.class);

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsJSON.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field)
                .setDataType("json");
        if (field.isAnnotationPresent(DefaultValue.class)) {
            log.warn(
                    "Field {} is annotated with @IsJSON and @DefaultValue, but MySQL JSON columns "
                            + "cannot declare a DEFAULT value (prior to 8.0.13). The default value will be ignored. "
                            + "Populate the value in your application layer instead.",
                    field.getName());
            builder.setDefaultValue(null);
        }
        return builder;
    }
}
