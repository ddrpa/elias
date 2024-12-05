package cc.ddrpa.dorian.elias.core.spec;

import static org.reflections.ReflectionUtils.Fields;

import cc.ddrpa.dorian.elias.core.annotation.DefaultValue;
import cc.ddrpa.dorian.elias.core.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable.Index;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUID;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUIDAsStr;
import cc.ddrpa.dorian.elias.core.annotation.types.CharLength;
import cc.ddrpa.dorian.elias.core.annotation.types.Decimal;
import cc.ddrpa.dorian.elias.core.annotation.types.TypeOverride;
import cc.ddrpa.dorian.elias.core.annotation.preset.IsJSON;
import cc.ddrpa.dorian.elias.core.annotation.types.UseText;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        // java.io.Serial 在 Java 11 中不可用，且该注解不会在运行时出现，无法用于判断是否忽略
        List<ColumnSpec> columns = fields.stream()
            .filter(field -> !field.getName().equalsIgnoreCase("serialVersionUID"))
            .map(f -> {
                Class<?> fClazz = f.getDeclaringClass();
                int depth = 0;
                while (fClazz != null) {
                    depth++;
                    fClazz = fClazz.getSuperclass();
                }
                return Pair.of(depth, f);
            })
            .sorted(Comparator.comparingInt(Pair::getLeft))
            .map(Pair::getRight)
            .map(SpecMaker::processField)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (columns.stream().filter(ColumnSpec::isPrimaryKey).count() > 1) {
            throw new IllegalStateException(
                "Multiple primary keys found in class: " + clazz.getName());
        }
        tableSpec.setColumns(columns);
        // 处理索引定义
        EliasTable eliasTableAnnotation = clazz.getAnnotation(EliasTable.class);
        if (eliasTableAnnotation != null && eliasTableAnnotation.indexes().length > 0) {
            Set<String> columnNameSet = columns.stream()
                .map(ColumnSpec::getName)
                .collect(Collectors.toSet());
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
            indexName = indexAnnotation.unique() ? "uk_" : "idx_";
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
        try {
            if (field.isAnnotationPresent(javax.validation.constraints.NotNull.class) ||
                field.isAnnotationPresent(javax.validation.constraints.NotEmpty.class) ||
                field.isAnnotationPresent(javax.validation.constraints.NotBlank.class)
            ) {
                columnSpec.setNullable(false);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotNull.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotEmpty.class)
            ) {
                columnSpec.setNullable(false);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        // DefaultValue 注解修饰的属性
        if (field.isAnnotationPresent(DefaultValue.class)) {
            DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
            columnSpec.setDefaultValue(defaultValue.value());
        }
        // 获取 column 的名称
        columnSpec.setName(getColumnName(field));
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableField} 注解且 Value 有效，设置为列名
         */
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableFieldAnnotation = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(tableFieldAnnotation.value())) {
                columnSpec.setName(tableFieldAnnotation.value());
            }
        }
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableId} 注解，设置为主键
         */
        if (field.isAnnotationPresent(TableId.class)) {
            TableId tableId = field.getAnnotation(TableId.class);
            if (StringUtils.isNoneBlank(tableId.value())) {
                columnSpec.setName(tableId.value());
            }
        }
        Pair<String, Long> columnType = getColumnType(field);
        /**
         * 如果字段有 {@link cc.ddrpa.dorian.elias.core.annotation.types.Decimal} 注解，设置为 DECIMAL 类型
         */
        if (field.getType().equals(BigDecimal.class)) {
            int precision = ConstantsPool.BIG_DECIMAL_DEFAULT_PRECISION;
            int scale = ConstantsPool.BIG_DECIMAL_DEFAULT_SCALE;
            if (field.isAnnotationPresent(Decimal.class)) {
                Decimal decimalAnnotation = field.getAnnotation(Decimal.class);
                precision = decimalAnnotation.precision();
                scale = decimalAnnotation.scale();
            }
            columnSpec.setDataType("decimal")
                .setPrecisionAndScale(precision, scale);
        } else {
            columnSpec.setDataType(columnType.getLeft())
                .setLength(columnType.getRight());
        }
        return columnSpec;
    }

    /**
     * 推断 column 的类型
     * <p>
     * FEAT_NEED
     * <p>
     * 根据列名推断字符类型列的长度，例如名称中带有 url / script / json 等字样
     *
     * @param field
     * @return
     */
    private static Pair<String, Long> getColumnType(Field field) {
        if (field.isAnnotationPresent(IsUUIDAsStr.class)) {
            return Pair.of("char", 36L);
        }
        if (field.isAnnotationPresent(IsUUID.class)) {
            return Pair.of("binary", 16L);
        }
        /**
         * 如果有 {@link TypeOverride} 注解，使用注解中的类型
         */
        if (field.isAnnotationPresent(TypeOverride.class)) {
            TypeOverride typeOverrideAnnotation = field.getAnnotation(TypeOverride.class);
            String overrideType = typeOverrideAnnotation.type().toLowerCase();
            long overrideLength = typeOverrideAnnotation.length();
            // FEAT_NEEDED 这里没有检查长度是否合法，类型是否支持
            return Pair.of(overrideType, overrideLength);
        }
        /**
         * 如果有 {@link UseText} 注解，根据估算的长度选择类型
         */
        if (field.isAnnotationPresent(UseText.class)) {
            UseText useTextAnnotation = field.getAnnotation(UseText.class);
            if (useTextAnnotation.estimated() <= ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH) {
                return Pair.of("varchar",
                    useTextAnnotation.estimated() > 0L
                        ? useTextAnnotation.estimated()
                        : ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH);
            } else if (useTextAnnotation.estimated() <= ConstantsPool.TEXT_MAX_CHARACTER_LENGTH) {
                return Pair.of("text", null);
            } else if (useTextAnnotation.estimated()
                < ConstantsPool.MEDIUMTEXT_MAX_CHARACTER_LENGTH) {
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
                charLengthAnnotation.length() > 0L ? charLengthAnnotation.length() : 255L;
            if (estimatedLength > ConstantsPool.VARCHAR_MAX_CHARACTER_LENGTH) {
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
                // override 使用 unsigned tinyint 类型表示布尔值
                return Pair.of("tinyint unsigned", null);
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
                return Pair.of("decimal", null);
            case "Date":
            case "java.sql.Timestamp":
                return Pair.of("datetime", null);
            case "char":
            case "java.lang.Character":
                return Pair.of("char", 1L);
            case "java.sql.Blob":
                return Pair.of("blob", 64000L);
            case "java.sql.Clob":
                // override，拉到上限
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