package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MIME 类型，映射为 VARCHAR(length)
 * <p>
 * 默认长度 127（RFC 6838 type/subtype 各 64 字符上限）
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsMimeType {

    int length() default 127;
}
