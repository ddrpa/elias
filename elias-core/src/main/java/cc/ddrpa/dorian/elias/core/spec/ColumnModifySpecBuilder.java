package cc.ddrpa.dorian.elias.core.spec;

import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;

public class ColumnModifySpecBuilder {

    private ColumnModifySpecBuilder() {
    }

    /**
     * <ul>
     *     <li>不允许将可空字段转换为非空字段（不考虑配置了 default value 的情况）</li>
     *     <li>如果转换的目标类型不是 TEXT 类型，则不允许转换前的长度大于转换后的长度</li>
     *     <li>转换目标类型必须符合无损转换要求</li>
     *     <li></li>
     * </ul>
     *
     * @param mismatch 列规范不匹配信息
     * @return 列修改规范
     */
    public static ColumnModifySpec build(ColumnSpecMismatch mismatch) {
        ColumnModifySpec columnModifySpec = new ColumnModifySpec();
        ColumnSpec expectedSpec = mismatch.getExpectedColumnSpec();

        // 设置完整列信息
        columnModifySpec.setColumnType(expectedSpec.getColumnType());
        columnModifySpec.setNullable(expectedSpec.isNullable());
        columnModifySpec.setDefaultValue(expectedSpec.getDefaultValue());

        // 设置修改标记
        if (mismatch.isColumnTypeMismatch()) {
            columnModifySpec.setAlterColumnType(true);
            if (mismatch.isDataTypeMismatch()) {
                // 如果存在类型转换且类型转换不是无损的
                if (!isLosslessDataTypeMigrate(
                        mismatch.getActualDataType(),
                        mismatch.getExpectedDataType())) {
                    // 检查是否允许降低数据类型精度
                    columnModifySpec.addWarning(
                            "* Reducing the size of a data type—like converting BIGINT to INT or DATETIME to DATE can cause truncation or loss of precision.");
                }
            } else if (mismatch.isLengthMismatch()) {
                // 类型保持不变，但长度不一致
                if ((mismatch.getExpectedDataType().endsWith("text")
                        || mismatch.getExpectedDataType().equals("blob")
                        || mismatch.getExpectedDataType().endsWith("char"))
                        && (mismatch.getExpectedLength() < mismatch.getActualLength())) {
                    // 如果是 BLOB/CHAR/TEXT 类型，则新的长度必须大于旧的长度
                    // 检查是否允许数据范围缩小
                    columnModifySpec.addWarning(
                            "* Reducing the length of CHAR, BLOB, or TEXT columns can result in data truncation.");
                }
            }
        }
        if (mismatch.isNullableMismatch()) {
            columnModifySpec.setAlterNullable(true);
            // 将可空字段转换为非空字段可能会失败
            if (mismatch.getActualNullable()) {
                // 检查是否允许 nullable 修改为 not null
                columnModifySpec.addWarning(
                        "* Setting a nullable column to NOT NULL may lead to constraint violations if any records contain null values.");
            }
        }
        if (mismatch.isDefaultValueMismatch()) {
            columnModifySpec.setAlterDefaultValue(true);
        }
        return columnModifySpec;
    }

    /**
     * 一般不会产生数据丢失问题的类型转换
     * <p>
     * <ul>
     *     <li>从较小的整数类型转换为较大的整数类型</li>
     *     <li>从从较小的 TEXT 类型到较大的 TEXT 类型</li>
     *     <li>从 DATE 到 DATETIME（TIME 部分是 00:00:00）</li>
     *     <li>从较小的浮点类型转换为较大的浮点类型</li>
     *     <li>布尔类型转换到整数类型</li>
     *     <li>非 BLOB 类型向 TEXT 类型转换</li>
     * </ul>
     *
     * @param fromDataType 原数据类型
     * @param toDataType   目标数据类型
     * @return
     */
    private static boolean isLosslessDataTypeMigrate(String fromDataType, String toDataType) {
        switch (fromDataType) {
            case "tinyint":
                return toDataType.endsWith("int") || toDataType.equals("mediumint")
                        || toDataType.equals("int") || toDataType.equals("bigint");
            case "smallint":
                return toDataType.equals("mediumint") || toDataType.equals(
                        "int") || toDataType.equals("bigint");
            case "mediumint":
                return toDataType.equals("int") || toDataType.equals("bigint");
            case "int":
                return toDataType.equals("bigint");
            case "text":
                return toDataType.equals("mediumtext") || toDataType.equals("longtext");
            case "mediumtext":
                return toDataType.equals("longtext");
            case "date":
                return toDataType.equals("datetime");
            case "float":
                return toDataType.equals("double");
            case "boolean":
                return toDataType.endsWith("int");
        }
        return toDataType.endsWith("text") && !fromDataType.equals("blob");
    }
}