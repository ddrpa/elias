package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * UUID 作为字符串存储，使用该注解要求字段按 CHAR(36) 类型存储
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsUUIDAsStr {

}