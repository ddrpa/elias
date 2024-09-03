package cc.ddrpa.dorian.elias.spring.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elias.validate")
public class EliasProperties {

    /**
     * 开启 schema 检查
     */
    private boolean enable = false;
    /**
     * 扫描行为
     */
    private ScanProperties scan = new ScanProperties();
    /**
     * 发现定义与数据库实际情况有差异时不停止运行
     * <p>
     * 不建议，可能存在不止一个差异，而检查器在发现某些差异后会跳过这张表的后续检查
     */
    private boolean stopOnMismatch = false;
    /**
     * 自动应用增量型修改
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

        public List<String> getIncludes() {
            return includes;
        }

        public ScanProperties setIncludes(List<String> includes) {
            this.includes = includes;
            return this;
        }
    }
}