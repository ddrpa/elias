package cc.ddrpa.dorian.elias.annotation.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求字段按 LONGTEXT 存储
 * <p>
 * 如果这个列用来存储序列化的 JSON 或是一些预计文本量比较大的数据，建议使用这个注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AsLongText {

}
