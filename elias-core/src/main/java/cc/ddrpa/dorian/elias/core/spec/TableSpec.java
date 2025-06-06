package cc.ddrpa.dorian.elias.core.spec;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TableSpec {

    private String name;
    private List<ColumnSpec> columns = new ArrayList<>();
    private List<IndexSpec> indexes = new ArrayList<>();
    private List<SpatialIndexSpec> spatialIndexSpecs = new ArrayList<>();
}