package cc.ddrpa.dorian.elias.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateTable {

    /**
     * 手动关闭表生成
     *
     * @return
     */
    boolean enable() default true;

    /**
     * 声明索引
     *
     * @return
     */
    Index[] indexes() default {};
}
