package cc.ddrpa.dorian.elias.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the automatic database column type inference for a field.
 * 
 * <p>This annotation has higher priority than automatic type inference but lower priority
 * than primary key declarations ({@code @TableId}). For VARCHAR and CHAR types, the length
 * represents character length; for other types it represents display length.
 * 
 * <p>Useful when the default type mapping doesn't meet specific requirements:
 * <pre>{@code
 * @TypeOverride(type = "TEXT")
 * private String longDescription;
 * 
 * @TypeOverride(type = "DECIMAL", length = 10)
 * private BigDecimal price;
 * }</pre>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeOverride {

    /**
     * Database column type to use instead of inferred type.
     * 
     * @return SQL column type (e.g., "TEXT", "DECIMAL", "LONGBLOB")
     */
    String type();

    /**
     * Length/precision for the column type.
     * 
     * <p>For character types (VARCHAR, CHAR): character length<br>
     * For numeric types: display length/precision<br>
     * Use -1 to omit length specification.
     * 
     * @return column length or -1 for default/no length
     */
    long length() default -1L;
}
