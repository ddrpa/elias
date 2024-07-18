package cc.ddrpa.dorian.elias.generator;

import static org.reflections.ReflectionUtils.Fields;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import cc.ddrpa.dorian.elias.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import cc.ddrpa.dorian.elias.annotation.types.AsLongText;
import cc.ddrpa.dorian.elias.annotation.types.GivenLength;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQLSchemaGenerator extends SchemaGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MySQLSchemaGenerator.class);

    private final String packageName;
    private final String schemaName;

    private boolean dropIfExists;
    private final Set<Class> classes = new HashSet<>(5);

    public MySQLSchemaGenerator(String packageName, String schemaName) {
        super("");
        this.packageName = packageName;
        this.schemaName = schemaName;
    }

    public MySQLSchemaGenerator addAllAnnotatedClass() {
        Reflections reflections = new Reflections(this.packageName);
        Set<Class<?>> annotated = reflections.get(
            SubTypes.of(TypesAnnotated.with(GenerateTable.class)).asClass());
        annotated.forEach(clazz -> logger.info("Found annotated class: {}", clazz.getName()));
        classes.addAll(annotated);
        return this;
    }

    public <T> MySQLSchemaGenerator addClass(Class<T> clazz) {
        classes.add(clazz);
        logger.info("Added class: {}", clazz.getName());
        return this;
    }

    public MySQLSchemaGenerator setOutputFile(String outputFile) throws IOException {
        List<String> tableDSLList = classes.stream().map(clazz -> {
            logger.info("Processing class: {}", clazz.getName());
            TableSpec tableSpec = processClass(clazz);
            return dsl(tableSpec);
        }).toList();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (String dsl : tableDSLList) {
                fos.write(dsl.getBytes(StandardCharsets.UTF_8));
                fos.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        return this;
    }

    private <T> TableSpec processClass(Class<T> clazz) {
        TableSpec tableSpec = new TableSpec();
        tableSpec.setSchemaName(this.schemaName);
        tableSpec.setTableName(getTableName(clazz));
        Set<Field> fields = ReflectionUtils.get(Fields.of(clazz));
        List<ColumnSpec> columns = fields.stream().map(this::processField)
            .filter(Objects::nonNull).toList();
        if (columns.stream().filter(ColumnSpec::isPrimaryKey).count() > 1) {
            throw new IllegalStateException(
                "Multiple primary keys found in class: " + clazz.getName());
        }
        tableSpec.setColumns(columns);
        return tableSpec;
    }

    private ColumnSpec processField(Field field) {
        logger.info("process field: {}", field.getName());
        // 如果字段有 EliasIgnore 注解，忽略
        if (field.isAnnotationPresent(EliasIgnore.class)) {
            return null;
        }
        ColumnSpec columnSpec = new ColumnSpec();
        // 如果字段有 com.baomidou.mybatisplus.annotation.TableId 注解，设置为主键
        if (field.isAnnotationPresent(TableId.class)) {
            columnSpec.setPrimaryKey(true);
            TableId tableId = field.getAnnotation(TableId.class);
            if (IdType.AUTO.equals(tableId.type()) || IdType.NONE.equals(tableId.type())) {
                columnSpec.setAutoIncrement(true);
            }
        }
        // 获取 column 的名称
        columnSpec.setColumnName(getColumnName(field));
        // 推断 column 的类型
        Pair<String, Integer> columnType = getColumnType(field);
        columnSpec.setColumnType(columnType.getLeft());
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
        // 如果有 AsLongText 注解，使用 LONGTEXT
        if (field.isAnnotationPresent(AsLongText.class)) {
            return Pair.of("LONGTEXT", 0);
        }
        // 如果有 GivenLength 注解
        if (field.isAnnotationPresent(GivenLength.class)) {
            GivenLength givenLength = field.getAnnotation(GivenLength.class);
            if (givenLength.fixedLength()) {
                return Pair.of("CHAR", givenLength.length());
            } else {
                return Pair.of("VARCHAR", givenLength.length());
            }
        }
        // 如果是枚举类型
        if (field.getType().isEnum()) {
            return Pair.of("TINYINT", 0);
        }
        String fieldType = field.getType().getName();
        // 处理 java.time.* 下的一些类型
        if (fieldType.equalsIgnoreCase("java.time.LocalDate")) {
            return Pair.of("DATE", 0);
        }
        if (fieldType.equalsIgnoreCase("java.time.LocalDateTime")) {
            return Pair.of("DATETIME", 0);
        }
        // see https://docs.oracle.com/cd/E19501-01/819-3659/gcmaz/
        if (field.getType().isArray()) {
            switch (field.getType().getSimpleName()) {
                case "byte[]", "java.lang.Byte[]" -> Pair.of("BLOB", 64000);
                case "char[]", "java.lang.Character[]" -> Pair.of("TEXT", 64000);
            }
        }
        return switch (fieldType) {
            case "boolean", "java.lang.Boolean" -> Pair.of("TINYINT", 1);
            case "int", "java.lang.Integer" -> Pair.of("INT", 0);
            case "long", "java.lang.Long", "java.math.BigInteger" -> Pair.of("BIGINT", 0);
            case "float", "java.lang.Float" -> Pair.of("FLOAT", 0);
            case "double", "java.lang.Double" -> Pair.of("DOUBLE", 0);
            case "short", "java.lang.Short", "byte", "java.lang.Byte" -> Pair.of("SMALLINT", 0);
            case "java.lang.Number", "java.math.BigDecimal" -> Pair.of("DECIMAL", 38);
            case "Date", "java.sql.Timestamp" -> Pair.of("DATETIME", 0);
            case "char", "java.lang.Character" -> Pair.of("CHAR", 1);
            case "java.sql.Blob" -> Pair.of("BLOB", 64000);
            case "java.sql.Clob" -> Pair.of("TEXT", 64000);
            case "java.sql.Date" -> Pair.of("DATE", 0);
            case "java.sql.Time" -> Pair.of("TIME", 0);
            default -> Pair.of("VARCHAR", 255);
        };
    }

    private String dsl(TableSpec tableSpec) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ")
            .append(tableSpec.getSchemaName()).append(".").append(tableSpec.getTableName())
            .append(" (\n");
        tableSpec.getColumns().forEach(columnSpec -> {
            sb.append("  ").append(columnSpec.getColumnName()).append(" ")
                .append(columnSpec.getColumnType());
            if (columnSpec.getLength() > 0) {
                sb.append("(").append(columnSpec.getLength()).append(")");
            }
            if (columnSpec.isPrimaryKey()) {
                sb.append(" PRIMARY KEY");
            }
            sb.append(",\n");
        });
        sb.deleteCharAt(sb.length() - 2);
        sb.append(");");
        return sb.toString();
    }
}