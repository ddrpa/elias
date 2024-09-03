package cc.ddrpa.dorian.elias.core.validation.mismatch.impl;

import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ColumnSpecMismatch implements ISpecMismatch {

    private Boolean columnTypeMismatch = false;
    private Boolean dataTypeMismatch = false;
    private Boolean lengthMismatch = false;

    private String expectedDataType;
    private String actualDataType;
    private Long expectedLength;
    private Long actualLength;
    private String expectedColumnType;
    private String actualColumnType;

    private Boolean nullableMismatch = false;
    private Boolean expectedNullable;
    private Boolean actualNullable;

    private Boolean defaultValueMismatch = false;
    private String expectedDefaultValue;
    private String actualDefaultValue;

    private String tableName;
    private String columnName;

    public ColumnSpecMismatch columnTypeMismatch(
        String expectedColumnType,
        String actualColumnType,
        String expectedDataType,
        String actualDataType,
        Long expectedLength,
        Long actualLength) {
        this.expectedColumnType = expectedColumnType;
        this.actualColumnType = actualColumnType;
        this.expectedDataType = expectedDataType;
        this.actualDataType = actualDataType;
        this.expectedLength = expectedLength;
        this.actualLength = actualLength;
        this.columnTypeMismatch = true;
        this.dataTypeMismatch = !Objects.equals(expectedDataType, actualDataType);
        this.lengthMismatch = !Objects.equals(expectedLength, actualLength);
        return this;
    }

    public ColumnSpecMismatch addNullableMismatch(Boolean expectedNullable,
        Boolean actualNullable) {
        this.expectedNullable = expectedNullable;
        this.actualNullable = actualNullable;
        nullableMismatch = true;
        return this;
    }

    public ColumnSpecMismatch addDefaultValueMismatch(String expectedDefaultValue,
        String actualDefaultValue) {
        this.expectedDefaultValue = expectedDefaultValue;
        this.actualDefaultValue = actualDefaultValue;
        this.defaultValueMismatch = true;
        return this;
    }

    @Override
    public String errorMessage() {
        List<String> mismatchMessages = new ArrayList<>(2);
        if (columnTypeMismatch) {
            mismatchMessages.add(
                String.format("* Column type not match: expected '%s', actual '%s'",
                    expectedColumnType, actualColumnType));
        }
        if (nullableMismatch) {
            mismatchMessages.add(
                String.format("* Different nullable property: expected '%s', actual '%s'",
                    expectedNullable ? "YES" : "NO", actualNullable ? "YES" : "NO"));
        }
        if (defaultValueMismatch) {
            mismatchMessages.add(String.format("* Default value not match: expected %s, actual %s",
                Objects.isNull(expectedDefaultValue) ? "<null>"
                    : String.format("'%s'", expectedDefaultValue),
                Objects.isNull(actualDefaultValue) ? "<null>"
                    : String.format("'%s'", actualDefaultValue)));
        }
        return String.format("Column `%s` in table `%s` has specification mismatch:\n%s",
            columnName, tableName, mismatchMessages.stream().collect(Collectors.joining("\n")));
    }

    public Boolean isColumnTypeMismatch() {
        return columnTypeMismatch;
    }

    public Boolean isDataTypeMismatch() {
        return dataTypeMismatch;
    }

    public Boolean isLengthMismatch() {
        return lengthMismatch;
    }

    public String getExpectedDataType() {
        return expectedDataType;
    }

    public String getActualDataType() {
        return actualDataType;
    }

    public long getExpectedLength() {
        return expectedLength;
    }

    public long getActualLength() {
        return actualLength;
    }

    public String getExpectedColumnType() {
        return expectedColumnType;
    }

    public String getActualColumnType() {
        return actualColumnType;
    }

    public boolean isNullableMismatch() {
        return nullableMismatch;
    }

    public Boolean getActualNullable() {
        return actualNullable;
    }

    public Boolean getExpectedNullable() {
        return expectedNullable;
    }

    public boolean isDefaultValueMismatch() {
        return defaultValueMismatch;
    }

    public String getExpectedDefaultValue() {
        return expectedDefaultValue;
    }

    public String getActualDefaultValue() {
        return actualDefaultValue;
    }

    public String getTableName() {
        return tableName;
    }

    public ColumnSpecMismatch setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public String getColumnName() {
        return columnName;
    }

    public ColumnSpecMismatch setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }
}