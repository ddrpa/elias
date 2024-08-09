package cc.ddrpa.dorian.elias.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = EliasProperties.PREFIX)
public class EliasProperties {

    public static final String PREFIX = "elias";

    private boolean enable = false;
    private List<String> packages;

    public boolean isEnable() {
        return enable;
    }

    public EliasProperties setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public List<String> getPackages() {
        return packages;
    }

    public EliasProperties setPackages(List<String> packages) {
        this.packages = packages;
        return this;
    }
}