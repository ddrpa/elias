package cc.ddrpa.dorian.elias.generator;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ColumnSpec {

    private String columnName;
    private String columnType;
    private int length;
    private boolean isNullable;
    private String comment;
    private boolean isPrimaryKey = false;
    private boolean autoIncrement = false;
}