package cc.ddrpa.dorian.elias.spec;

import static org.reflections.ReflectionUtils.Fields;

import cc.ddrpa.dorian.elias.annotation.EliasTable;
import cc.ddrpa.dorian.elias.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.annotation.Index;
import cc.ddrpa.dorian.elias.annotation.types.AsLongText;
import cc.ddrpa.dorian.elias.annotation.types.DefaultValue;
import cc.ddrpa.dorian.elias.annotation.types.Length;
import cc.ddrpa.dorian.elias.annotation.types.TypeOverride;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecMaker {

    private static final Logger logger = LoggerFactory.getLogger(SpecMaker.class);

    /**
     * 将 Java 类转换为 TableSpec
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static TableSpec makeTableSpec(Class<?> clazz) {
        TableSpec tableSpec = new TableSpec();
        tableSpec.setName(getTableName(clazz));
        // 处理类成员
        Set<Field> fields = ReflectionUtils.get(Fields.of(clazz));
        List<ColumnSpec> columns = fields.stream().map(SpecMaker::processField)
            .filter(Objects::nonNull).collect(Collectors.toList());
        if (columns.stream().filter(ColumnSpec::isPrimaryKey).count() > 1) {
            throw new IllegalStateException(
                "Multiple primary keys found in class: " + clazz.getName());
        }
        tableSpec.setColumns(columns);
        // 处理索引定义
        EliasTable eliasTableAnnotation = clazz.getAnnotation(EliasTable.class);
        Set<String> columnNameSet = columns.stream()
            .map(ColumnSpec::getName)
            .collect(Collectors.toSet());
        if (eliasTableAnnotation != null && eliasTableAnnotation.indexes().length > 0) {
            List<IndexSpec> indexSpecs = Arrays.stream(eliasTableAnnotation.indexes())
                .map(annotation -> processIndex(annotation, columnNameSet))
                .collect(Collectors.toList());
            tableSpec.setIndexes(indexSpecs);
        }
        return tableSpec;
    }

    private static IndexSpec processIndex(Index indexAnnotation, Set<String> columnNameSet) {
        String indexName;
        String columnList = indexAnnotation.columnList();
        if (StringUtils.isBlank(columnList)) {
            logger.error("Index column list is empty");
            throw new IllegalStateException("Index column list is empty");
        }
        if (StringUtils.isNoneBlank(indexAnnotation.name())) {
            indexName = indexAnnotation.name();
        } else {
            indexName = "idx_";
            if (indexAnnotation.unique()) {
                indexName += "unique_";
            }
            List<String> columns = Arrays.stream(
                    indexAnnotation.columnList().split(","))
                .map(columnSpec -> columnSpec.trim().split(" ")[0])
                .collect(Collectors.toList());
            columns.stream().forEach(column -> {
                if (!columnNameSet.contains(column)) {
                    logger.error("Index column not found: {}", column);
                    throw new IllegalStateException("Index column not found: " + column);
                }
            });
            indexName += columns.stream().collect(Collectors.joining("_"));
        }
        return new IndexSpec()
            .setName(indexName)
            .setUnique(indexAnnotation.unique())
            .setColumnList(indexAnnotation.columnList());
    }

    private static ColumnSpec processField(Field field) {
        logger.info("process field: {}", field.getName());
        // 如果字段有 EliasIgnore 注解，忽略
        if (field.isAnnotationPresent(EliasIgnore.class)) {
            return null;
        }
        // 如果字段有 com.baomidou.mybatisplus.annotation.TableField 注解且 exist 为 false，忽略
        if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableField.class)) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (!tableField.exist()) {
                return null;
            }
        }
        ColumnSpec columnSpec = new ColumnSpec();
        // 如果字段有 com.baomidou.mybatisplus.annotation.TableId 注解，设置为主键
        if (field.isAnnotationPresent(TableId.class)) {
            columnSpec.setPrimaryKey(true);
            columnSpec.setNullable(false);
            TableId tableId = field.getAnnotation(TableId.class);
            if (IdType.AUTO.equals(tableId.type()) || IdType.NONE.equals(tableId.type())) {
                columnSpec.setAutoIncrement(true);
            }
        }
        // 如果字段有 com.baomidou.mybatisplus.annotation.TableLogic 注解，设置 defaultValue 为 0
        // 这一功能有待商榷
        if (field.isAnnotationPresent(TableLogic.class)) {
            columnSpec.setDefaultValue("0");
        }
        // 有 javax.validation.constraints.NotBlank, javax.validation.constraints.NotEmpty, javax.validation.constraints.NotNull 注解
        // 设置为非空
        if (field.isAnnotationPresent(NotNull.class) ||
            field.isAnnotationPresent(NotEmpty.class) ||
            field.isAnnotationPresent(NotBlank.class)) {
            columnSpec.setNullable(false);
        }
        // DefaultValue 注解修饰的属性
        if (field.isAnnotationPresent(DefaultValue.class)) {
            DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
            columnSpec.setDefaultValue(defaultValue.value());
        }
        // 获取 column 的名称
        columnSpec.setName(getColumnName(field));
        Pair<String, Integer> columnType = getColumnType(field);
        columnSpec.setType(columnType.getLeft());
        columnSpec.setLength(columnType.getRight());
        return columnSpec;
    }

    /**
     * 推断 column 的类型
     *
     * @param field
     * @return
     */
    private static Pair<String, Integer> getColumnType(Field field) {
        // 如果有 TypeOverride 注解，使用注解中的类型
        if (field.isAnnotationPresent(TypeOverride.class)) {
            TypeOverride typeOverrideAnnotation = field.getAnnotation(TypeOverride.class);
            return Pair.of(typeOverrideAnnotation.type(), typeOverrideAnnotation.length());
        }
        // 如果有 AsLongText 注解，使用 longtext
        if (field.isAnnotationPresent(AsLongText.class)) {
            return Pair.of("longtext", 0);
        }
        // 如果有 Length 注解
        if (field.isAnnotationPresent(Length.class)) {
            Length length = field.getAnnotation(Length.class);
            if (length.fixedLength()) {
                return Pair.of("char", length.length());
            } else {
                return Pair.of("varchar", length.length());
            }
        }
        // 如果是枚举类型
        if (field.getType().isEnum()) {
            return Pair.of("tinyint", 0);
        }
        String fieldType = field.getType().getName();
        // 处理 java.time.* 下的一些类型
        if (fieldType.equalsIgnoreCase("java.time.LocalDate")) {
            return Pair.of("date", 0);
        }
        if (fieldType.equalsIgnoreCase("java.time.LocalDateTime")) {
            return Pair.of("datetime", 0);
        }
        // see https://docs.oracle.com/cd/E19501-01/819-3659/gcmaz/
        if (field.getType().isArray()) {
            switch (field.getType().getSimpleName()) {
                case "byte[]":
                case "java.lang.Byte[]":
                    return Pair.of("blob", 64000);
                case "char[]":
                case "java.lang.Character[]":
                    return Pair.of("text", 64000);
            }
        }
        switch (fieldType) {
            case "boolean":
            case "java.lang.Boolean":
                return Pair.of("tinyint", 1);
            case "int":
            case "java.lang.Integer":
                return Pair.of("int", 0);
            case "long":
            case "java.lang.Long":
            case "java.math.BigInteger":
                return Pair.of("bigint", 0);
            case "float":
            case "java.lang.Float":
                return Pair.of("float", 0);
            case "double":
            case "java.lang.Double":
                return Pair.of("double", 0);
            case "short":
            case "java.lang.Short":
            case "byte":
            case "java.lang.Byte":
                return Pair.of("smallint", 0);
            case "java.lang.Number":
            case "java.math.BigDecimal":
                return Pair.of("decimal", 38);
            case "Date":
            case "java.sql.Timestamp":
                return Pair.of("datetime", 0);
            case "char":
            case "java.lang.Character":
                return Pair.of("char", 1);
            case "java.sql.Blob":
                return Pair.of("blob", 64000);
            case "java.sql.Clob":
                return Pair.of("text", 64000);
            case "java.sql.Date":
                return Pair.of("date", 0);
            case "java.sql.Time":
                return Pair.of("time", 0);
            default:
                return Pair.of("varchar", 255);
        }
    }


    private static String camelCaseToSnakeCase(String text) {
        Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text);
        return m.replaceAll(match -> "_" + match.group().toLowerCase());
    }

    private static String getTableName(Class clazz) {
        if (clazz.isAnnotationPresent(TableName.class)) {
            TableName tableName = (TableName) clazz.getAnnotation(TableName.class);
            if (StringUtils.isNoneBlank(tableName.value())) {
                return tableName.value();
            }
        } else if (clazz.isAnnotationPresent(EliasTable.class)) {
            EliasTable eliasTable = (EliasTable) clazz.getAnnotation(
                EliasTable.class);
            if (StringUtils.isNoneBlank(eliasTable.tablePrefix())) {
                return eliasTable.tablePrefix() + camelCaseToSnakeCase(
                    clazz.getSimpleName()).toLowerCase();
            }
        }
        return camelCaseToSnakeCase(clazz.getSimpleName()).toLowerCase();
    }

    private static String getColumnName(Field field) {
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(tableField.value())) {
                return tableField.value();
            }
        }
        return camelCaseToSnakeCase(field.getName());
    }
}