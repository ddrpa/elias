package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FEAT_NEED
 * <p>
 * MySQL 5.7.8 之后支持 JSON 类型
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsJSON {

    /**
     * 设置该列为空时的默认值为 JSON 对象还是 JSON 数组
     *
     * @return
     */
    EmptyType emptyAs();

    public static enum EmptyType {
        OBJECT,
        ARRAY;
    }
}