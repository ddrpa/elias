package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * URL，映射为 VARCHAR(length)；当 length 超过 VARCHAR 上限时降级为 TEXT
 * <p>
 * 默认长度 2048
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsURL {

    int length() default 2048;
}
