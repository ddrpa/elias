package cc.ddrpa.dorian.elias.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateTable {

    /**
     * 可以手动关闭或开启表的生成
     *
     * @return
     */
    boolean enable() default true;

    /**
     * 表名前缀，例如 'tbl_'
     *
     * @return
     */
    String tablePrefix() default "";

    /**
     * 声明索引，和 Jakarta Persistence 的行为差不多
     *
     * @return
     */
    Index[] indexes() default {};
}
