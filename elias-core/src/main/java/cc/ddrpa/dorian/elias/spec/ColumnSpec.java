package cc.ddrpa.dorian.elias.spec;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ColumnSpec {

    private String name;
    private String type;
    private int length;
    private boolean nullable = true;
    private String comment;
    private boolean primaryKey = false;
    private boolean autoIncrement = false;
    private String defaultValue;
}