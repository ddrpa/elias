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

    /**
     * 为几何类型的非空字段自动创建空间索引
     *
     * @return
     */
    boolean autoSpatialIndexForGeometry() default true;

    Index[] spatialIndexes() default {};

    /**
     * 行为类似 javax.persistence.Index，用于声明索引
     *
     * @see <a
     * href="https://jakarta.ee/specifications/persistence/2.2/apidocs/javax/persistence/">Jakarta
     * Persistence API - Index</a>
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Index {

        /**
         * The name of the index; defaults to a provider-generated name.
         */
        String name() default "";

        /**
         * The names of the columns to be included in the index, in order. The syntax of the
         * columnList element is a column_list, as follows:
         * <pre>
         * column::= index_column [,index_column]*
         * index_column::= column_name [ASC | DESC]
         * </pre>
         */
        String columns();

        /**
         * Whether the index is unique, default false
         */
        boolean unique() default false;
    }
}
