package cc.ddrpa.dorian.elias.spring.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Elias Spring Boot integration.
 * 
 * <p>Controls schema validation behavior including package scanning,
 * mismatch handling, and auto-fix options. Properties are bound from
 * the {@code elias.validate} prefix in application configuration.
 */
@ConfigurationProperties(prefix = "elias.validate")
public class EliasProperties {

    /**
     * Enables schema validation at application startup.
     */
    private boolean enable = false;
    /**
     * Package scanning configuration for entity discovery.
     */
    private ScanProperties scan = new ScanProperties();
    /**
     * Continues application startup even when schema mismatches are found.
     * 
     * <p>Not recommended as multiple issues may exist and some checks are skipped after errors.
     */
    private boolean stopOnMismatch = false;
    /**
     * Automatically applies safe schema modifications to fix mismatches.
     */
    private boolean autoFix = false;

    public boolean isEnable() {
        return enable;
    }

    public EliasProperties setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public ScanProperties getScan() {
        return scan;
    }

    public EliasProperties setScan(
        ScanProperties scan) {
        this.scan = scan;
        return this;
    }

    public boolean isStopOnMismatch() {
        return stopOnMismatch;
    }

    public EliasProperties setStopOnMismatch(boolean stopOnMismatch) {
        this.stopOnMismatch = stopOnMismatch;
        return this;
    }

    public boolean isAutoFix() {
        return autoFix;
    }

    public EliasProperties setAutoFix(boolean autoFix) {
        this.autoFix = autoFix;
        return this;
    }

    public static class ScanProperties {

        /**
         * 需要指定路径下的 Java class
         */
        private List<String> includes = new ArrayList<>(1);
        /**
         * 如果类使用 {@link com.baomidou.mybatisplus.annotation.TableName} 修饰，也会被记录
         */
        private Boolean acceptMybatisPlusTableNameAnnotation = true;

        public List<String> getIncludes() {
            return includes;
        }

        public ScanProperties setIncludes(List<String> includes) {
            this.includes = includes;
            return this;
        }

        public Boolean getAcceptMybatisPlusTableNameAnnotation() {
            return acceptMybatisPlusTableNameAnnotation;
        }

        public ScanProperties setAcceptMybatisPlusTableNameAnnotation(
            Boolean acceptMybatisPlusTableNameAnnotation) {
            this.acceptMybatisPlusTableNameAnnotation = acceptMybatisPlusTableNameAnnotation;
            return this;
        }
    }
}