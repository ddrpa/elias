package cc.ddrpa.dorian.elias.spec;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TableSpec {

    private String name;
    private List<ColumnSpec> columns = new ArrayList<>(5);
    private List<IndexSpec> indexes = new ArrayList<>(2);
}