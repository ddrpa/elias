package cc.ddrpa.dorian.elias.core.clfixture;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import jakarta.validation.constraints.NotNull;

/**
 * Loaded via a child ClassLoader in tests so {@code @NotNull} is a different {@link Class}
 * instance than the one on the parent/plugin ClassLoader.
 */
@EliasTable
public class JakartaNotNullEntity {

    @NotNull
    private String departName;

    public String getDepartName() {
        return departName;
    }
}
