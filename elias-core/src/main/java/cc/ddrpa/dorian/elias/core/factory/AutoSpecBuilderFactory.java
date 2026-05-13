package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.SpecUtils;
import cc.ddrpa.dorian.elias.core.annotation.preset.Auto;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * 处理 {@link Auto} 注解：依据字段的 Java 类型与字段名（转 snake_case 后小写）做双重判断，
 * 选择合适的 MySQL 列类型；未命中任何规则时兜底为 {@code varchar(255)}。
 */
public class AutoSpecBuilderFactory implements SpecBuilderFactory {

    // String 长文本关键字（命中 → text）
    private static final Set<String> LONG_TEXT_TOKENS = Set.of(
            "description", "content", "bio", "biography", "remark",
            "note", "comment", "body", "summary", "detail"
    );

    // String 字段类型 → 各类（短）字符串的命中关键字
    private static final List<StringRule> STRING_RULES = List.of(
            new StringRule(Set.of("email"), "varchar", 254L),
            new StringRule(Set.of("phone", "mobile", "tel"), "varchar", 32L),
            new StringRule(Set.of("url", "link", "href", "website"), "varchar", 2048L),
            new StringRule(Set.of("slug"), "varchar", 255L),
            new StringRule(Set.of("mime_type", "content_type"), "varchar", 127L),
            new StringRule(Set.of("timezone", "tz_name"), "varchar", 64L),
            new StringRule(Set.of("country_code"), "char", 2L),
            new StringRule(Set.of("currency_code"), "char", 3L),
            new StringRule(Set.of("language_code", "locale"), "varchar", 35L),
            new StringRule(Set.of("color", "colour"), "char", 7L),
            new StringRule(Set.of("mac_address"), "char", 17L),
            new StringRule(Set.of("uuid", "guid"), "char", 36L),
            new StringRule(Set.of("ip_address", "ip_addr"), "varchar", 45L),
            new StringRule(Set.of("password", "secret", "token", "api_key"), "char", 64L)
    );

    // byte[] 字段类型 → 二进制规则
    private static final List<BinaryRule> BYTE_ARRAY_RULES = List.of(
            new BinaryRule(Set.of("sha512"), "binary", 64L),
            new BinaryRule(Set.of("sha384"), "binary", 48L),
            new BinaryRule(Set.of("sha256", "sha_256"), "binary", 32L),
            new BinaryRule(Set.of("sha1", "sha_1"), "binary", 20L),
            new BinaryRule(Set.of("md5"), "binary", 16L),
            new BinaryRule(Set.of("hash", "digest"), "binary", 32L),
            new BinaryRule(Set.of("uuid", "guid"), "binary", 16L),
            new BinaryRule(Set.of("ip_address", "ip_addr"), "varbinary", 16L)
    );

    // BigDecimal 字段类型 → 金额/百分比规则
    private static final Set<String> MONEY_TOKENS = Set.of(
            "price", "amount", "cost", "fee", "balance", "money", "salary", "revenue", "total"
    );
    private static final Set<String> PERCENT_TOKENS = Set.of(
            "percent", "percentage", "rate", "ratio"
    );

    // 整数类型常量名
    private static final Set<String> LONG_TYPES = Set.of(
            "long", "java.lang.Long", "java.math.BigInteger"
    );
    private static final Set<String> INT_TYPES = Set.of(
            "int", "java.lang.Integer"
    );
    private static final Set<String> SMALL_INT_TYPES = Set.of(
            "short", "java.lang.Short", "byte", "java.lang.Byte"
    );
    private static final Set<String> FLOAT_TYPES = Set.of(
            "float", "java.lang.Float", "double", "java.lang.Double"
    );
    private static final Set<String> BOOLEAN_TYPES = Set.of(
            "boolean", "java.lang.Boolean"
    );
    private static final Set<String> DATE_TYPES = Set.of(
            "java.time.LocalDate", "java.sql.Date"
    );
    private static final Set<String> TIME_TYPES = Set.of(
            "java.time.LocalTime", "java.sql.Time"
    );
    private static final Set<String> DATETIME_TYPES = Set.of(
            "java.time.LocalDateTime", "java.time.Instant",
            "java.time.ZonedDateTime", "java.time.OffsetDateTime",
            "java.sql.Timestamp", "java.util.Date"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(Auto.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        String name = SpecUtils.camelCaseToSnakeCase(field.getName()).toLowerCase();
        String typeName = field.getType().getName();
        Class<?> fieldType = field.getType();

        if (typeName.equals("java.lang.String")) {
            inferString(builder, name);
            return applyCharLengthOverride(builder, field);
        }
        if (fieldType.isArray() && typeName.equals("[B")) {
            inferByteArray(builder, name);
            return builder;
        }
        if (LONG_TYPES.contains(typeName)) {
            if (name.endsWith("_id")) {
                builder.setDataType("bigint").setLength(20L)
                        .setColumnType("bigint(20) unsigned");
            } else {
                builder.setDataType("bigint").setLength(20L);
            }
            return builder;
        }
        if (INT_TYPES.contains(typeName)) {
            builder.setDataType("int");
            return builder;
        }
        if (SMALL_INT_TYPES.contains(typeName)) {
            builder.setDataType("smallint");
            return builder;
        }
        if (typeName.equals("java.math.BigDecimal")) {
            inferBigDecimal(builder, name);
            return builder;
        }
        if (BOOLEAN_TYPES.contains(typeName)) {
            builder.setDataType("tinyint").setLength(1L);
            return builder;
        }
        if (FLOAT_TYPES.contains(typeName)) {
            builder.setDataType("double");
            return builder;
        }
        if (DATE_TYPES.contains(typeName)) {
            builder.setDataType("date");
            return builder;
        }
        if (TIME_TYPES.contains(typeName)) {
            builder.setDataType("time");
            return builder;
        }
        if (DATETIME_TYPES.contains(typeName)) {
            builder.setDataType("datetime");
            return builder;
        }
        if (fieldType.isEnum()) {
            builder.setDataType("smallint");
            return builder;
        }
        // 兜底
        builder.setDataType("varchar").setLength(255L);
        return applyCharLengthOverride(builder, field);
    }

    private void inferString(ColumnSpecBuilder builder, String name) {
        for (StringRule rule : STRING_RULES) {
            if (containsAny(name, rule.tokens)) {
                builder.setDataType(rule.dataType).setLength(rule.length);
                return;
            }
        }
        if (containsAny(name, LONG_TEXT_TOKENS)) {
            builder.setDataType("text");
            return;
        }
        builder.setDataType("varchar").setLength(255L);
    }

    private void inferByteArray(ColumnSpecBuilder builder, String name) {
        for (BinaryRule rule : BYTE_ARRAY_RULES) {
            if (containsAny(name, rule.tokens)) {
                builder.setDataType(rule.dataType).setLength(rule.length);
                return;
            }
        }
        builder.setDataType("blob");
    }

    private void inferBigDecimal(ColumnSpecBuilder builder, String name) {
        if (containsAny(name, MONEY_TOKENS)) {
            builder.setDataType("decimal").setPrecision(19).setScale(4);
            return;
        }
        if (containsAny(name, PERCENT_TOKENS)) {
            builder.setDataType("decimal").setPrecision(5).setScale(2);
            return;
        }
        builder.setDataType("decimal").setPrecision(10).setScale(2);
    }

    private static boolean containsAny(String name, Set<String> tokens) {
        for (String token : tokens) {
            if (name.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private record StringRule(Set<String> tokens, String dataType, long length) {
    }

    private record BinaryRule(Set<String> tokens, String dataType, long length) {
    }
}
