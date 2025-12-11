package cc.ddrpa.dorian.elias.core.validation;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 列的属性，只检查类型、长度、是否可空、默认值
 */
public class ColumnProperties {

    // 列名
    private final String name;
    // 例如 varchar
    private final String dataType;
    // 例如 varchar(255)，有时候还会包含 unsigned 等信息
    private final String columnType;
    // 是否可空
    private final Boolean nullable;
    // 是字符类型
    private final Boolean characterType;
    // 是 TEXT 类型
    private final Boolean textType;
    // 是 Blob 类型
    private final Boolean blobType;
    // 是 Binary 类型
    private final Boolean binaryType;
    // 数据存储长度
    private final Optional<Long> dataLength;
    // 默认值
    private final Optional<String> defaultValueAsString;

    public ColumnProperties(Map<String, Object> rawProperties) {
        this.name = rawProperties.get("COLUMN_NAME").toString();
        this.dataType = rawProperties.get("DATA_TYPE").toString().toLowerCase();
        this.columnType = rawProperties.get("COLUMN_TYPE").toString().toLowerCase();
        this.nullable = rawProperties.get("IS_NULLABLE").toString().equalsIgnoreCase("YES");
        this.characterType = dataType.endsWith("char");
        this.textType = dataType.endsWith("text");
        this.blobType = dataType.equals("blob");
        this.binaryType = dataType.endsWith("binary");
        if (Objects.nonNull(rawProperties.get("CHARACTER_MAXIMUM_LENGTH"))) {
            this.dataLength = Optional.of(
                    Long.parseLong(rawProperties.get("CHARACTER_MAXIMUM_LENGTH").toString()));
        } else {
            this.dataLength = Optional.empty();
        }
        if (Objects.nonNull(rawProperties.get("COLUMN_DEFAULT"))) {
            this.defaultValueAsString = Optional.of(rawProperties.get("COLUMN_DEFAULT").toString());
        } else {
            this.defaultValueAsString = Optional.empty();
        }
    }

    public String getName() {
        return name;
    }

    /**
     * 检查列的属性是否与给定的 ColumnSpec 一致
     *
     * @param columnSpec
     * @return
     */
    public Optional<ColumnSpecMismatch> validate(ColumnSpec columnSpec) {
        boolean columnSpecMismatchFlag = false;
        ColumnSpecMismatch columnSpecMismatch = new ColumnSpecMismatch();
        if (dataType.equals(columnSpec.getDataType())) {
            if (characterType || blobType || binaryType) {
                // 如果 DataType 一致，且类型是 Blob、Char 或 Binary 类型，才检查长度是否有变化
                // TEXT 类型不设置数据长度，不检查
                // 数值类型是可视长度，不检查
                if (!Objects.equals(columnSpec.getLength(), dataLength.orElse(null))) {
                    columnSpecMismatch.columnTypeMismatch(columnSpec.getColumnType(), columnType,
                            columnSpec.getDataType(), dataType,
                            columnSpec.getLength(), dataLength.orElse(null));
                    columnSpecMismatchFlag = true;
                }
            }
        } else {
            // DataType 不一致
            columnSpecMismatch.columnTypeMismatch(columnSpec.getColumnType(), columnType,
                    columnSpec.getDataType(), dataType,
                    columnSpec.getLength(), dataLength.orElse(null));
            columnSpecMismatchFlag = true;
        }
        // 检查 nullable 属性是否一致
        if (!Objects.equals(nullable, columnSpec.isNullable())) {
            columnSpecMismatch.addNullableMismatch(columnSpec.isNullable(), nullable);
            columnSpecMismatchFlag = true;
        }
        // 检查默认值是否一致
        if (defaultValueAsString.isEmpty()) {
            if (StringUtils.isNotEmpty(columnSpec.getDefaultValue())) {
                columnSpecMismatch.addDefaultValueMismatch(columnSpec.getDefaultValue(), null);
                columnSpecMismatchFlag = true;
            }
        } else if (!Objects.equals(columnSpec.getDefaultValue(), defaultValueAsString.get())) {
            columnSpecMismatch.addDefaultValueMismatch(
                    columnSpec.getDefaultValue(),
                    defaultValueAsString.get());
            columnSpecMismatchFlag = true;
        }
        if (columnSpecMismatchFlag) {
            columnSpecMismatch.setExpectedColumnSpec(columnSpec);
            return Optional.of(columnSpecMismatch);
        } else {
            return Optional.empty();
        }
    }
}