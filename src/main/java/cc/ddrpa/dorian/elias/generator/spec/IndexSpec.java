package cc.ddrpa.dorian.elias.generator.spec;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IndexSpec {

    private String name;
    private boolean unique;
    private String columnList;
}
