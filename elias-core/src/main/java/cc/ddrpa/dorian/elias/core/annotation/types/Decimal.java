package cc.ddrpa.dorian.elias.core.annotation.types;

import cc.ddrpa.dorian.elias.core.ConstantsPool;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置数据库中 Decimal 类型的精度，关于这两个值的作用，推荐阅读 <a href="https://stackoverflow.com/a/35436935">BigDecimal,
 * precision and scale - Stackoverflow</a>
 * <p>
 * 对于非 BigDecimal 类型的字段，该注解无效
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Decimal {

    int precision() default ConstantsPool.BIG_DECIMAL_DEFAULT_PRECISION;

    int scale() default ConstantsPool.BIG_DECIMAL_DEFAULT_SCALE;
}