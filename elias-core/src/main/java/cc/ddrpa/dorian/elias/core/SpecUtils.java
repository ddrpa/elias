package cc.ddrpa.dorian.elias.core;

import cc.ddrpa.dorian.elias.core.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods for database schema generation and field processing.
 * 
 * <p>Provides naming convention conversions, field filtering logic, and other
 * helper functions used throughout the schema generation process. This class
 * handles integration with both Elias-specific annotations and MyBatis-Plus annotations.
 */
public class SpecUtils {

    private SpecUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Determines if a field should be excluded from database column generation.
     * 
     * <p>Fields are ignored if they are:
     * <ul>
     * <li>Annotated with {@link EliasIgnore}
     * <li>Static or transient fields
     * <li>MyBatis-Plus fields marked as non-existent ({@code exist = false})
     * </ul>
     * 
     * @param field the field to check
     * @return true if the field should be ignored, false if it should generate a column
     */
    public static boolean shouldIgnoreColumn(Field field) {
        // 如果字段是静态的，忽略
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        // 忽略 serialVersionUID
        if (field.getName().equalsIgnoreCase("serialVersionUID")) {
            return false;
        }
        /**
         * 如果字段有 {@link EliasIgnore 注解 }，忽略
         */
        if (field.isAnnotationPresent(EliasIgnore.class)) {
            return false;
        }
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableField} 注解且 exist 为 false，忽略
         */
        if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableField.class)) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (!tableField.exist()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts camelCase strings to snake_case database naming convention.
     * 
     * <p>Transforms Java field/class names to database-compatible names by inserting
     * underscores before uppercase letters and converting to lowercase.
     * 
     * @param text camelCase string to convert
     * @return snake_case version of the input
     */
    public static String camelCaseToSnakeCase(String text) {
        Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text);
        return m.replaceAll(match -> "_" + match.group().toLowerCase());
    }

    /**
     * Determines the database table name for a Java class.
     * 
     * <p>Naming priority:
     * <ol>
     * <li>{@link TableName} annotation value (MyBatis-Plus)
     * <li>{@link EliasTable} annotation value
     * <li>Class simple name converted to snake_case
     * </ol>
     * 
     * @param clazz the entity class
     * @return database table name
     */
    public static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(TableName.class)) {
            TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);
            if (StringUtils.isNoneBlank(tableNameAnnotation.value())) {
                return tableNameAnnotation.value();
            }
        } else if (clazz.isAnnotationPresent(EliasTable.class)) {
            EliasTable eliasTableAnnotation = clazz.getAnnotation(EliasTable.class);
            if (StringUtils.isNoneBlank(eliasTableAnnotation.tablePrefix())) {
                return eliasTableAnnotation.tablePrefix() + camelCaseToSnakeCase(
                    clazz.getSimpleName()).toLowerCase();
            }
        }
        return camelCaseToSnakeCase(clazz.getSimpleName()).toLowerCase();
    }

    /**
     * Determines the database column name for a Java field.
     * 
     * <p>Naming priority:
     * <ol>
     * <li>{@link TableField} annotation value (MyBatis-Plus)
     * <li>Field name converted to snake_case
     * </ol>
     * 
     * @param field the entity field
     * @return database column name
     */
    public static String getColumnName(Field field) {
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableFieldAnnotation = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(tableFieldAnnotation.value())) {
                return tableFieldAnnotation.value();
            }
        }
        return camelCaseToSnakeCase(field.getName());
    }
}