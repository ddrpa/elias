package cc.ddrpa.dorian.elias.generator.export;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes JDBC / H2 / MySQL type names so Spec vs metadata comparison stays stable
 * when the export baseline runs on H2 {@code MODE=MySQL}.
 */
public final class JdbcTypeNormalizer {

    private static final Set<String> CHAR_LIKE = Set.of(
            "char", "varchar", "text", "tinytext", "mediumtext", "longtext",
            "binary", "varbinary", "blob", "tinyblob", "mediumblob", "longblob");

    private JdbcTypeNormalizer() {
    }

    public static String normalizeDataType(String rawTypeName) {
        if (rawTypeName == null || rawTypeName.isBlank()) {
            return "";
        }
        String t = rawTypeName.toLowerCase(Locale.ROOT).trim();
        // strip precision: varchar(255), timestamp(6)
        int paren = t.indexOf('(');
        if (paren > 0) {
            t = t.substring(0, paren).trim();
        }
        return switch (t) {
            case "character varying", "varchar2" -> "varchar";
            case "character", "nchar", "nvarchar" -> "char";
            case "character large object", "clob", "longvarchar" -> "text";
            case "binary varying", "longvarbinary" -> "varbinary";
            case "binary large object" -> "blob";
            case "integer", "int4", "signed", "unsigned" -> "int";
            case "int8", "bigserial" -> "bigint";
            case "int2", "serial" -> "smallint";
            case "int1" -> "tinyint";
            case "float4", "real" -> "float";
            case "float8", "double precision" -> "double";
            case "bool", "boolean", "bit" -> "tinyint";
            case "timestamp", "timestamptz", "timestamp with time zone",
                 "timestamp without time zone", "datetime2" -> "datetime";
            case "time with time zone", "time without time zone" -> "time";
            case "decimal", "numeric", "number" -> "decimal";
            case "json", "jsonb" -> "json";
            default -> t;
        };
    }

    /**
     * Whether {@link java.sql.DatabaseMetaData#getColumns} COLUMN_SIZE is meaningful for length checks.
     */
    public static boolean isLengthComparable(String normalizedDataType) {
        return CHAR_LIKE.contains(normalizedDataType);
    }

    public static void normalizeRow(Map<String, Object> row) {
        Object raw = row.get("DATA_TYPE");
        String normalized = normalizeDataType(raw == null ? null : raw.toString());
        row.put("DATA_TYPE", normalized);
        if (!isLengthComparable(normalized)) {
            row.put("CHARACTER_MAXIMUM_LENGTH", null);
        }
        Object columnType = row.get("COLUMN_TYPE");
        if (columnType == null || columnType.toString().isBlank()) {
            Object length = row.get("CHARACTER_MAXIMUM_LENGTH");
            if (length != null && isLengthComparable(normalized)) {
                row.put("COLUMN_TYPE", normalized + "(" + length + ")");
            } else {
                row.put("COLUMN_TYPE", normalized);
            }
        }
        row.put("COLUMN_DEFAULT", normalizeDefault(
                row.get("COLUMN_DEFAULT") == null ? null : row.get("COLUMN_DEFAULT").toString()));
    }

    /**
     * Strips H2/MySQL JDBC default decorations ({@code 'x'}, {@code NULL}, {@code x::VARCHAR}).
     */
    public static String normalizeDefault(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.trim();
        if (d.isEmpty() || "null".equalsIgnoreCase(d)) {
            return null;
        }
        if ((d.startsWith("'") && d.endsWith("'")) || (d.startsWith("\"") && d.endsWith("\""))) {
            d = d.substring(1, d.length() - 1);
        }
        int cast = d.indexOf("::");
        if (cast > 0) {
            d = d.substring(0, cast);
        }
        return d;
    }
}
