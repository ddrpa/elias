package cc.ddrpa.dorian.elias.core;

import cc.ddrpa.dorian.elias.core.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecUtils {

    private SpecUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 判断某个字段是否需要忽略
     *
     * @param field
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
            return tableField.exist();
        }
        return true;
    }

    /**
     * 驼峰转蛇形
     *
     * @param text
     * @return
     */
    public static String camelCaseToSnakeCase(String text) {
        Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text);
        return m.replaceAll(match -> "_" + match.group().toLowerCase());
    }

    /**
     * 推断表名
     *
     * @param clazz
     * @return
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
     * 推断列名
     *
     * @param field
     * @return
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