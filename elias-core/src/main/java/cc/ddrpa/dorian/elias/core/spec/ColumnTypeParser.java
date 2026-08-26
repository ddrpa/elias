package cc.ddrpa.dorian.elias.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * 解析 MySQL 风格列类型字符串，例如 {@code VARBINARY(16)}、{@code varchar(500)}、
 * {@code bigint(20) unsigned}、{@code decimal(10, 2)}。
 */
public final class ColumnTypeParser {

    private ColumnTypeParser() {
    }

    public record Parsed(
            String dataType,
            Optional<Long> length,
            Optional<Integer> precision,
            Optional<Integer> scale,
            boolean preserveColumnType
    ) {
    }

    public static Parsed parse(String rawType) {
        if (StringUtils.isBlank(rawType)) {
            throw new IllegalArgumentException("Column type must not be blank");
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        int open = normalized.indexOf('(');
        int close = open >= 0 ? normalized.indexOf(')', open + 1) : -1;

        String head = open >= 0 ? normalized.substring(0, open).trim() : normalized;
        String dataType = head.split("\\s+")[0];

        Optional<Long> length = Optional.empty();
        Optional<Integer> precision = Optional.empty();
        Optional<Integer> scale = Optional.empty();
        if (open >= 0 && close > open) {
            String inside = normalized.substring(open + 1, close).trim();
            if ("decimal".equals(dataType) || "numeric".equals(dataType)) {
                String[] parts = inside.split(",");
                if (parts.length >= 1 && isDigits(parts[0].trim())) {
                    precision = Optional.of(Integer.parseInt(parts[0].trim()));
                    if (parts.length >= 2 && isDigits(parts[1].trim())) {
                        scale = Optional.of(Integer.parseInt(parts[1].trim()));
                    } else {
                        scale = Optional.of(0);
                    }
                }
            } else {
                length = parseLengthFromColumnType(normalized);
            }
        }

        String after = close >= 0 ? normalized.substring(close + 1).trim() : "";
        boolean preserveColumnType = StringUtils.isNotBlank(after) || head.contains(" ");
        return new Parsed(dataType, length, precision, scale, preserveColumnType);
    }

    /**
     * 从 {@code varchar(255)} / {@code varbinary(512)} / {@code decimal(10,2)} 等类型串中解析第一个长度数字。
     */
    public static Optional<Long> parseLengthFromColumnType(String columnType) {
        if (StringUtils.isBlank(columnType)) {
            return Optional.empty();
        }
        int open = columnType.indexOf('(');
        int close = columnType.indexOf(')', open + 1);
        if (open < 0 || close <= open) {
            return Optional.empty();
        }
        String inside = columnType.substring(open + 1, close).trim();
        if (inside.isEmpty()) {
            return Optional.empty();
        }
        String number = inside.split("[,\\s]+")[0];
        if (!isDigits(number)) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(number));
    }

    private static boolean isDigits(String value) {
        return StringUtils.isNotBlank(value) && value.chars().allMatch(Character::isDigit);
    }
}
