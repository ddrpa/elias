package cc.ddrpa.dorian.elias.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 行为类似 {@link javax.persistence.Index}，用于声明索引
 *
 * @see <a
 * href="https://jakarta.ee/specifications/persistence/2.2/apidocs/javax/persistence/">Jakarta
 * Persistence API - Index</a>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {

    /**
     * The name of the index; defaults to a provider-generated name.
     */
    String name() default "";

    /**
     * The names of the columns to be included in the index, in order. The syntax of the columnList
     * element is a column_list, as follows:
     * <pre>
     * column::= index_column [,index_column]*
     * index_column::= column_name [ASC | DESC]
     * </pre>
     */
    String columnList();

    /**
     * Whether the index is unique, default false
     */
    boolean unique() default false;
}
