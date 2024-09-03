package cc.ddrpa.dorian.elias.core.annotation.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 覆盖列的类型，注意对 VARCHAR 和 CHAR 类型，length 表示的是字符长度，其他情况下则表示可视长度
 * <p>
 * 优先级高于类型推断，但低于 Id 之类的声明
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeOverride {

    /**
     * 列类型
     *
     * @return
     */
    String type();

    /**
     * 列长度
     *
     * @return
     */
    long length();
}
