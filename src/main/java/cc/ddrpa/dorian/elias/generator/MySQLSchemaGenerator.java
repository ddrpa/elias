package cc.ddrpa.dorian.elias.generator;

import static org.reflections.ReflectionUtils.Fields;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import cc.ddrpa.dorian.elias.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import cc.ddrpa.dorian.elias.annotation.Index;
import cc.ddrpa.dorian.elias.annotation.types.AsLongText;
import cc.ddrpa.dorian.elias.annotation.types.DefaultValue;
import cc.ddrpa.dorian.elias.annotation.types.Length;
import cc.ddrpa.dorian.elias.annotation.types.TypeOverride;
import cc.ddrpa.dorian.elias.generator.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.generator.spec.IndexSpec;
import cc.ddrpa.dorian.elias.generator.spec.TableSpec;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQLSchemaGenerator extends SchemaGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MySQLSchemaGenerator.class);
    private final Set<Class> classes = new HashSet<>(5);
    private final Template template;

    private String database;
    private boolean dropIfExists = true;

    public MySQLSchemaGenerator() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class",
            ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        template = velocityEngine.getTemplate("mysql.vm");
    }

    public MySQLSchemaGenerator enableDropIfExists() {
        this.dropIfExists = true;
        return this;
    }

    public MySQLSchemaGenerator disableDropIfExists() {
        this.dropIfExists = false;
        return this;
    }

    public MySQLSchemaGenerator setDatabase(String database) {
        this.database = database;
        return this;
    }

    /**
     * 添加指定包下的所有带有 GenerateTable 注解的类
     *
     * @param packageName
     * @return
     */
    public MySQLSchemaGenerator addAllAnnotatedClass(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.get(
            SubTypes.of(TypesAnnotated.with(GenerateTable.class)).asClass());
        annotated.forEach(clazz -> {
            GenerateTable generateTable = clazz.getAnnotation(GenerateTable.class);
            if (Objects.isNull(generateTable)) {
                // 如果 DTO 类继承了需要生成表的实体类，那么它会被反射找出来，但是获取这个注解时为 null
                // 不需要为这种 DTO 生成表
                return;
            }
            if (!generateTable.enable()) {
                // 手动关闭表生成
                return;
            }
            logger.info("Found annotated class: {}", clazz.getName());
            classes.add(clazz);
        });
        return this;
    }

    /**
     * 添加一个特定的类
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> MySQLSchemaGenerator addClass(Class<T> clazz) {
        classes.add(clazz);
        logger.info("Added class: {}", clazz.getName());
        return this;
    }

    /**
     * 导出 SQL 文件
     *
     * @param outputFile
     * @throws IOException
     */
    public void export(String outputFile) throws IOException {
        List<String> tableDSLList = classes.stream()
            .sorted(Comparator.comparing(Class::getSimpleName))
            .map(clazz -> {
                logger.info("Processing class: {}", clazz.getName());
                TableSpec tableSpec = processClass(clazz);
                return ddl(tableSpec);
            })
            .toList();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (String dsl : tableDSLList) {
                fos.write(dsl.getBytes(StandardCharsets.UTF_8));
                fos.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 将 Java 类转换为 TableSpec
     *
     * @param clazz
     * @param <T>
     * @return
     */
    private <T> TableSpec processClass(Class<T> clazz) {
        TableSpec tableSpec = new TableSpec();
        tableSpec.setName(getTableName(clazz));
        tableSpec.setDropIfExists(this.dropIfExists);
        // 处理类成员
        Set<Field> fields = ReflectionUtils.get(Fields.of(clazz));
        List<ColumnSpec> columns = fields.stream().map(this::processField)
            .filter(Objects::nonNull).toList();
        if (columns.stream().filter(ColumnSpec::isPrimaryKey).count() > 1) {
            throw new IllegalStateException(
                "Multiple primary keys found in class: " + clazz.getName());
        }
        tableSpec.setColumns(columns);
        // 处理索引定义
        GenerateTable generateTableAnnotation = clazz.getAnnotation(GenerateTable.class);
        Set<String> columnNameSet = columns.stream()
            .map(ColumnSpec::getName)
            .collect(Collectors.toSet());
        if (generateTableAnnotation != null && generateTableAnnotation.indexes().length > 0) {
            List<IndexSpec> indexSpecs = Arrays.stream(generateTableAnnotation.indexes())
                .map(annotation -> processIndex(annotation, columnNameSet))
                .toList();
            tableSpec.setIndexes(indexSpecs);
        }
        return tableSpec;
    }

    private IndexSpec processIndex(Index indexAnnotation, Set<String> columnNameSet) {
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
                .toList();
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

    private ColumnSpec processField(Field field) {
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
        // 如果有 jakarta.validation.constraints.NotBlank, jakarta.validation.constraints.NotEmpty, jakarta.validation.constraints.NotNull 注解
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
    private Pair<String, Integer> getColumnType(Field field) {
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
                case "byte[]", "java.lang.Byte[]" -> Pair.of("blob", 64000);
                case "char[]", "java.lang.Character[]" -> Pair.of("text", 64000);
            }
        }
        return switch (fieldType) {
            case "boolean", "java.lang.Boolean" -> Pair.of("tinyint", 1);
            case "int", "java.lang.Integer" -> Pair.of("int", 0);
            case "long", "java.lang.Long", "java.math.BigInteger" -> Pair.of("bigint", 0);
            case "float", "java.lang.Float" -> Pair.of("float", 0);
            case "double", "java.lang.Double" -> Pair.of("double", 0);
            case "short", "java.lang.Short", "byte", "java.lang.Byte" -> Pair.of("smallint", 0);
            case "java.lang.Number", "java.math.BigDecimal" -> Pair.of("decimal", 38);
            case "Date", "java.sql.Timestamp" -> Pair.of("datetime", 0);
            case "char", "java.lang.Character" -> Pair.of("char", 1);
            case "java.sql.Blob" -> Pair.of("blob", 64000);
            case "java.sql.Clob" -> Pair.of("text", 64000);
            case "java.sql.Date" -> Pair.of("date", 0);
            case "java.sql.Time" -> Pair.of("time", 0);
            default -> Pair.of("varchar", 255);
        };
    }

    private String ddl(TableSpec tableSpec) {
        VelocityContext context = new VelocityContext();
        context.put("t", tableSpec);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}