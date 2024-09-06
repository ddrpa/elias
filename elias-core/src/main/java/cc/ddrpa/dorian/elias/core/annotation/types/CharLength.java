package cc.ddrpa.dorian.elias.core.annotation.types;

import cc.ddrpa.dorian.elias.core.spec.ConstantsPool;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置字符串类型字段的长度
 * <p>
 * 该注解仅对字符串类型的字段有效，优先级高于类型推断，但低于 {@link com.baomidou.mybatisplus.annotation.TableId} 之类的声明，该注解不会覆盖
 * {@link UseText} 的声明
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CharLength {

    /**
     * 为 true 时声明为 CHAR 类型，否则为 VARCHAR
     *
     * @return
     */
    boolean fixed() default false;

    /**
     * 最大长度，默认为 255(ConstantsPool.VARCHAR_DEFAULT_CHARACTER_LENGTH)
     * <p>
     * 不能大于 5000(ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH)，否则会被推断为 TEXT 类型
     *
     * @return
     */
    long length() default ConstantsPool.VARCHAR_DEFAULT_CHARACTER_LENGTH;
}
