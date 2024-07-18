package cc.ddrpa.dorian.elias.generator;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TableSpec {

    private boolean dropIfExists = false;
    private boolean createIfNotExists = true;
    private String schemaName;
    private String tableName;
    private List<ColumnSpec> columns = new ArrayList<>(5);
}