package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP 地址，按二进制存储
 * <ul>
 *   <li>{@link IPVersion#ALL} - VARBINARY(16)，兼容 IPv4 / IPv6</li>
 *   <li>{@link IPVersion#V4} - BINARY(4)</li>
 *   <li>{@link IPVersion#V6} - BINARY(16)</li>
 * </ul>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsIP {

    IPVersion version() default IPVersion.ALL;

    enum IPVersion {
        ALL,
        V4,
        V6
    }
}
