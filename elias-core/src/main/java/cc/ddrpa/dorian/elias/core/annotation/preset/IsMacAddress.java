package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MAC 地址
 * <ul>
 *   <li>默认 BINARY(6)</li>
 *   <li>{@code asString=true} 时 CHAR(17)，存储形如 {@code aa:bb:cc:dd:ee:ff}</li>
 * </ul>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsMacAddress {

    boolean asString() default false;
}
