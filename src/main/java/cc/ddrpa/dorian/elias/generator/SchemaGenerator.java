package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public abstract class SchemaGenerator {

    static String camelCaseToSnakeCase(String text) {
        Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text);
        return m.replaceAll(match -> "_" + match.group().toLowerCase());
    }

    String getTableName(Class clazz) {
        if (clazz.isAnnotationPresent(TableName.class)) {
            TableName tableName = (TableName) clazz.getAnnotation(TableName.class);
            if (StringUtils.isNoneBlank(tableName.value())) {
                return tableName.value();
            }
        } else if (clazz.isAnnotationPresent(GenerateTable.class)) {
            GenerateTable generateTable = (GenerateTable) clazz.getAnnotation(GenerateTable.class);
            if (StringUtils.isNoneBlank(generateTable.tablePrefix())) {
                return generateTable.tablePrefix() + camelCaseToSnakeCase(
                    clazz.getSimpleName()).toLowerCase();
            }
        }
        return camelCaseToSnakeCase(clazz.getSimpleName()).toLowerCase();
    }

    String getColumnName(Field field) {
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(tableField.value())) {
                return tableField.value();
            }
        }
        return camelCaseToSnakeCase(field.getName());
    }
}