package cc.ddrpa.dorian.elias.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java class for database table generation and schema validation.
 * 
 * <p>This annotation serves two primary purposes:
 * <ul>
 * <li>During DDL generation: Indicates that Elias should create a table for this entity class</li>
 * <li>During runtime validation: Marks tables that should be checked for schema consistency</li>
 * </ul>
 * 
 * <p>The annotation supports advanced features like index definitions, spatial indexes for geometry
 * fields, and table name customization. Classes without this annotation are ignored during
 * entity discovery unless explicitly included via other mechanisms.
 * 
 * @see cc.ddrpa.dorian.elias.core.SpecMaker#makeTableSpec(Class)
 * @see cc.ddrpa.dorian.elias.core.EntitySearcher
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EliasTable {

    /**
     * Controls whether table generation is enabled for this entity.
     * 
     * @return true to generate/validate the table, false to skip this entity
     */
    boolean enable() default true;

    /**
     * Optional table name prefix to prepend to the generated table name.
     * 
     * @return prefix string (e.g., "tbl_", "app_") or empty string for no prefix
     */
    String tablePrefix() default "";

    /**
     * Regular index definitions for this table.
     * 
     * <p>Similar to Jakarta Persistence API index definitions. Each index can specify
     * column names, sort order, and uniqueness constraints.
     * 
     * @return array of index definitions
     */
    Index[] indexes() default {};

    /**
     * Automatically creates spatial indexes for non-nullable geometry columns.
     * 
     * @return true to auto-create spatial indexes, false to skip automatic creation
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
