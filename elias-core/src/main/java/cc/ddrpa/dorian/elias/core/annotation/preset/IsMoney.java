package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 货币金额，映射为 DECIMAL(precision, scale)
 * <p>
 * 默认 DECIMAL(19, 4)，可表示约 ±9.99×10^14 范围内 4 位小数
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsMoney {

    int precision() default 19;

    int scale() default 4;
}
