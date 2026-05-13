package cc.ddrpa.dorian.elias.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级唯一索引定义。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(UniqueIndexes.class)
public @interface UniqueIndex {

    /**
     * 索引名，留空时由 Elias 自动生成。
     */
    String name() default "";

    /**
     * 索引分组，留空表示独立单列索引；同组会组装为联合索引。
     */
    String group() default "";

    /**
     * 分组中的顺序，越小越靠前。
     */
    int pos() default 0;

    /**
     * 是否降序，默认 false 表示 ASC。
     */
    boolean desc() default false;
}
