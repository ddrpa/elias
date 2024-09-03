package cc.ddrpa.dorian.elias.core.spec;

import static org.reflections.ReflectionUtils.Fields;

import cc.ddrpa.dorian.elias.core.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.Index;
import cc.ddrpa.dorian.elias.core.annotation.types.CharLength;
import cc.ddrpa.dorian.elias.core.annotation.types.DefaultValue;
import cc.ddrpa.dorian.elias.core.annotation.types.TypeOverride;
import cc.ddrpa.dorian.elias.core.annotation.types.UseText;
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

    /**
     * 解析索引配置
     *
     * @param indexAnnotation
     * @param columnNameSet
     * @return
     */
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

    /**
     * 将类的属性转换为列定义
     *
     * @param field
     * @return
     */
    private static ColumnSpec processField(Field field) {
        logger.trace("process field: {}", field.getName());
        /**
         * 如果字段有 {@link EliasIgnore 注解 }，忽略
         */
        if (field.isAnnotationPresent(EliasIgnore.class)) {
            return null;
        }
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableField} 注解且 exist 为 false，忽略
         */
        if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableField.class)) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (!tableField.exist()) {
                return null;
            }
        }
        ColumnSpec columnSpec = new ColumnSpec();
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableId} 注解，设置为主键
         */
        if (field.isAnnotationPresent(TableId.class)) {
            columnSpec.setPrimaryKey(true);
            columnSpec.setNullable(false);
            TableId tableId = field.getAnnotation(TableId.class);
            if (IdType.AUTO.equals(tableId.type()) || IdType.NONE.equals(tableId.type())) {
                columnSpec.setAutoIncrement(true);
            }
        }
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableLogic} 注解，设置 defaultValue 为 0
         * <p>
         * NEED_CHECK 这一功能有待商榷，value 可以通过配置文件覆盖
         */
        if (field.isAnnotationPresent(TableLogic.class)) {
            columnSpec.setDefaultValue("0");
        }
        /**
         * 如果字段有 {@link javax.validation.constraints.NotBlank},
         * {@link javax.validation.constraints.NotEmpty},
         * {@link javax.validation.constraints.NotNull} 注解，设置为非空
         */
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
        Pair<String, Long> columnType = getColumnType(field);
        columnSpec.setColumnType(columnType.getLeft(), columnType.getRight());
        return columnSpec;
    }

    /**
     * 推断 column 的类型
     *
     * @param field
     * @return
     */
    private static Pair<String, Long> getColumnType(Field field) {
        /**
         * 如果有 {@link TypeOverride} 注解，使用注解中的类型
         */
        if (field.isAnnotationPresent(TypeOverride.class)) {
            TypeOverride typeOverrideAnnotation = field.getAnnotation(TypeOverride.class);
            String overrideType = typeOverrideAnnotation.type().toLowerCase();
            Long overrideLength = typeOverrideAnnotation.length();
            // FEAT_NEEDED 这里没有检查长度是否合法，类型是否支持
            return Pair.of(overrideType, overrideLength);
        }
        /**
         * 如果有 {@link UseText} 注解，根据估算的长度选择类型
         */
        if (field.isAnnotationPresent(UseText.class)) {
            UseText useTextAnnotation = field.getAnnotation(UseText.class);
            if (useTextAnnotation.estimated() < 16384L) {
                return Pair.of("varchar",
                    useTextAnnotation.estimated() > 0L ? useTextAnnotation.estimated() : 16383L);
            } else if (useTextAnnotation.estimated() < 65536L) {
                return Pair.of("text", null);
            } else if (useTextAnnotation.estimated() < 16_777_216L) {
                return Pair.of("mediumtext", null);
            } else {
                return Pair.of("longtext", null);
            }
        }
        /**
         * 如果有 {@link CharLength} 注解，根据注解中的长度选择类型
         */
        if (field.isAnnotationPresent(CharLength.class)) {
            CharLength charLengthAnnotation = field.getAnnotation(CharLength.class);
            long estimatedLength =
                charLengthAnnotation.length() > 0 ? charLengthAnnotation.length() : 255L;
            if (estimatedLength > 65536L) {
                return Pair.of("text", null);
            } else if (charLengthAnnotation.fixed()) {
                return Pair.of("char", charLengthAnnotation.length());
            } else {
                return Pair.of("varchar", charLengthAnnotation.length());
            }
        }
        // 如果是枚举类型
        if (field.getType().isEnum()) {
            return Pair.of("tinyint", 4L);
        }
        String fieldType = field.getType().getName();
        // 处理 java.time.* 下的一些类型
        if (fieldType.equalsIgnoreCase("java.time.LocalDate")) {
            return Pair.of("date", null);
        }
        if (fieldType.equalsIgnoreCase("java.time.LocalDateTime")) {
            return Pair.of("datetime", null);
        }
        // see https://docs.oracle.com/cd/E19501-01/819-3659/gcmaz/
        if (field.getType().isArray()) {
            switch (field.getType().getSimpleName()) {
                case "byte[]":
                case "java.lang.Byte[]":
                    return Pair.of("blob", 64000L);
                case "char[]":
                case "java.lang.Character[]":
                    // override，拉到上限，即 65535 个字符
                    return Pair.of("text", null);
            }
        }
        switch (fieldType) {
            case "boolean":
            case "java.lang.Boolean":
                return Pair.of("tinyint", 1L);
            case "int":
            case "java.lang.Integer":
                return Pair.of("int", null);
            case "long":
            case "java.lang.Long":
            case "java.math.BigInteger":
                // override，使用长整型存储雪花 ID 时，长度通常为 19 个字符
                return Pair.of("bigint", 20L);
            case "float":
            case "java.lang.Float":
                return Pair.of("float", null);
            case "double":
            case "java.lang.Double":
                return Pair.of("double", null);
            case "short":
            case "java.lang.Short":
            case "byte":
            case "java.lang.Byte":
                return Pair.of("smallint", null);
            case "java.lang.Number":
            case "java.math.BigDecimal":
                return Pair.of("decimal", 38L);
            case "Date":
            case "java.sql.Timestamp":
                return Pair.of("datetime", null);
            case "char":
            case "java.lang.Character":
                return Pair.of("char", 1L);
            case "java.sql.Blob":
                return Pair.of("blob", 64000L);
            case "java.sql.Clob":
                // override，拉到上限，即 65535 个字符
                return Pair.of("text", null);
            case "java.sql.Date":
                return Pair.of("date", null);
            case "java.sql.Time":
                return Pair.of("time", null);
            default:
                return Pair.of("varchar", 255L);
        }
    }

    /**
     * 驼峰转蛇形
     *
     * @param text
     * @return
     */
    private static String camelCaseToSnakeCase(String text) {
        Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(text);
        return m.replaceAll(match -> "_" + match.group().toLowerCase());
    }

    /**
     * 推断表名
     *
     * @param clazz
     * @return
     */
    private static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(TableName.class)) {
            TableName tableNameAnnotation = (TableName) clazz.getAnnotation(TableName.class);
            if (StringUtils.isNoneBlank(tableNameAnnotation.value())) {
                return tableNameAnnotation.value();
            }
        } else if (clazz.isAnnotationPresent(EliasTable.class)) {
            EliasTable eliasTableAnnotation = (EliasTable) clazz.getAnnotation(EliasTable.class);
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
    private static String getColumnName(Field field) {
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableFieldAnnotation = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(tableFieldAnnotation.value())) {
                return tableFieldAnnotation.value();
            }
        }
        return camelCaseToSnakeCase(field.getName());
    }
}