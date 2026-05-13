package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 百分比，映射为 DECIMAL(precision, scale)
 * <p>
 * 默认 DECIMAL(5, 2)，可表示 0.00 - 100.00（或负数与超过 100 的边界值）
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsPercentage {

    int precision() default 5;

    int scale() default 2;
}
