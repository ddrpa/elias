package cc.ddrpa.dorian.elias.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <ul>
 * <li>在创建 DDL 的阶段要求 elias 为该实体类类生成表</li>
 * <li>在运行阶段要求 elias 检查数据库中的表是否与定义一致</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EliasTable {

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