package cc.ddrpa.dorian.elias.generator.spec;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TableSpec {

    private boolean dropIfExists = false;
//    private boolean createIfNotExists = true;
//    private String database;
    private String name;
    private List<ColumnSpec> columns = new ArrayList<>(5);
    private List<IndexSpec> indexes = new ArrayList<>(2);
}