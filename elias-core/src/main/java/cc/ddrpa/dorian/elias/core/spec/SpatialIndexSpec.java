package cc.ddrpa.dorian.elias.core.spec;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SpatialIndexSpec {

    private String name;
    private String columns;
}