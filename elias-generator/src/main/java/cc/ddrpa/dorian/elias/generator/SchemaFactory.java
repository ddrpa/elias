package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.EntitySearcher;
import cc.ddrpa.dorian.elias.core.spec.SpecMaker;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SchemaFactory {

    private final Logger logger = LoggerFactory.getLogger(SchemaFactory.class);
    /**
     * 单独添加的类
     */
    private final Set<Class<?>> separatelyAddedClasses = new HashSet<>(5);
    private final EntitySearcher entitySearcher = new EntitySearcher();
    private boolean dropIfExists = false;

    public SchemaFactory dropIfExists(boolean enable) {
        this.dropIfExists = enable;
        return this;
    }

    /**
     * 添加指定包下的所有带有 EliasTable 注解的类
     *
     * @param packageName
     * @return
     */
    public SchemaFactory addAllAnnotatedClass(String packageName) {
        entitySearcher.addPackage(packageName);
        return this;
    }

    /**
     * 添加一个特定的类
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> SchemaFactory addClass(Class<T> clazz) {
        separatelyAddedClasses.add(clazz);
        return this;
    }

    /**
     * 导出 SQL 文件
     *
     * @param outputFile
     * @throws IOException
     */
    public void export(String outputFile) throws IOException {
        separatelyAddedClasses.addAll(entitySearcher.search());
        List<String> tableDSLList = separatelyAddedClasses.stream()
            .sorted(Comparator.comparing(Class::getSimpleName))
            .map(clazz -> {
                logger.trace("Processing class: {}", clazz.getName());
                TableSpec tableSpec = SpecMaker.makeTableSpec(clazz);
                return SQLGenerator.createTable(tableSpec, dropIfExists);
            })
            .collect(Collectors.toList());
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (String dsl : tableDSLList) {
                fos.write(dsl.getBytes(StandardCharsets.UTF_8));
                fos.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}