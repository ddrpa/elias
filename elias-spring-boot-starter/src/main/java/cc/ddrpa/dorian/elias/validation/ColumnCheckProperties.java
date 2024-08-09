package cc.ddrpa.dorian.elias.validation;

import cc.ddrpa.dorian.elias.spec.ColumnSpec;
import java.util.Map;

public class ColumnCheckProperties {

    private String name;
    private String dataType;
    private Integer length;
    private Boolean nullable;
    private String defaultValueAsString;

    public String getName() {
        return name;
    }

    public ColumnCheckProperties(Map<String, Object> rawProperties) {
        this.name = rawProperties.get("COLUMN_NAME").toString();
        this.dataType = rawProperties.get("DATA_TYPE").toString();
        this.length = rawProperties.get("CHARACTER_MAXIMUM_LENGTH") == null
            ? null
            : Integer.parseInt(rawProperties.get("CHARACTER_MAXIMUM_LENGTH").toString());
        this.nullable = rawProperties.get("IS_NULLABLE").toString().equalsIgnoreCase("YES");
        this.defaultValueAsString = rawProperties.get("COLUMN_DEFAULT") == null
            ? null
            : rawProperties.get("COLUMN_DEFAULT").toString();
    }

    public boolean validate(ColumnSpec columnSpec) {
        assert dataType.equals(columnSpec.getType());
        if (columnSpec.getLength() > 0) {
            assert length == columnSpec.getLength();
        }
        assert nullable == columnSpec.isNullable();
        if (columnSpec.getDefaultValue() != null) {
            assert defaultValueAsString.equals(columnSpec.getDefaultValue().toString());
        }
        return true;
    }
}