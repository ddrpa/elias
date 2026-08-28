package cc.ddrpa.dorian.elias.core.spec;

import org.apache.commons.lang3.StringUtils;

/**
 * 规范化列 comment，去掉会破坏多语句 DDL 执行（按 {@code ;} 拆分）的字符，
 * 并为 SQL 字符串字面量转义单引号。
 */
public final class ColumnComments {

    private ColumnComments() {
    }

    /**
     * 去掉分号与换行等危险字符；空白则返回 {@code null}。
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw
                .replace(";", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\u0000', ' ')
                .trim()
                .replaceAll(" +", " ");
        return StringUtils.isBlank(cleaned) ? null : cleaned;
    }

    /**
     * DDL COMMENT 子句用的内容：先 sanitize，再将 {@code '} 转义为 {@code ''}。
     */
    public static String forSqlLiteral(String raw) {
        String cleaned = sanitize(raw);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replace("'", "''");
    }
}
