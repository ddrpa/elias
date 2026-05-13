package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 让 Elias 根据字段的 Java 类型与字段名自动选择列类型。
 * <p>
 * 例如：
 * <ul>
 *   <li>{@code String emailAddress} → {@code varchar(254)}</li>
 *   <li>{@code Long userId} → {@code bigint(20) unsigned}（Snowflake 约定）</li>
 *   <li>{@code byte[] passwordHash} → {@code binary(32)}（默认 SHA-256 等长）</li>
 *   <li>{@code BigDecimal totalAmount} → {@code decimal(19,4)}</li>
 * </ul>
 * 未命中任何规则时兜底为 {@code varchar(255)}。
 * <p>
 * 注解优先级：本注解低于 {@link IsEmail}/{@link IsHash} 等具体的语义化注解；
 * 当字段同时声明 {@code @CharLength} 且推断结果为文本系时，会用 {@code @CharLength.length()} 覆盖长度。
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auto {
}
