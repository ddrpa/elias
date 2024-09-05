package cc.ddrpa.dorian.elias.core.annotation.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 如果这个列用来存储序列化的 JSON 或是一些预计文本量比较大的数据，建议使用该注解要求字段按 TEXT 类型存储
 * <p>
 * 优先级高于 {@link CharLength} 和 {@link TypeOverride}，但低于
 * {@link com.baomidou.mybatisplus.annotation.TableId} 之类的声明
 * <p>
 * estimated 表示预计的最大长度，elias 会根据这个长度来推断字段的类型，对一篇中等长度的文章来说，字符数量一般不会超过 2^14
 * <ul>
 *     <li>estimated &lt; 16384，推断为 VARCHAR(estimated)</li>
 *     <li>16384 &lt;= estimated &lt; 65536，推断为 TEXT</li>
 *     <li>65536 &lt;= estimated &lt; 16_777_216，推断为 MEDIUMTEXT</li>
 *     <li>16_777_216 &lt;= estimated，推断为 LONGTEXT</li>
 * </ul>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UseText {

    long estimated() default 16383L;
}
