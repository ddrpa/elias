package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.EntitySearcher;
import cc.ddrpa.dorian.elias.core.SpecMaker;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SchemaFactory {

    private final Logger logger = LoggerFactory.getLogger(SchemaFactory.class);
    private final Set<Class<?>> classes = new HashSet<>(5);
    private final EntitySearcher entitySearcher = new EntitySearcher();

    /**
     * 添加指定包下的所有符合条件的类
     *
     * @param packageRef
     * @return
     */
    public SchemaFactory addPackage(String packageRef) {
        entitySearcher.addPackage(packageRef);
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
        classes.add(clazz);
        return this;
    }

    /**
     * 额外搜索使用指定注解的类
     *
     * @param annotationClass
     * @param <A>
     * @return
     */
    public <A extends Annotation> SchemaFactory useAnnotation(Class<A> annotationClass) {
        entitySearcher.useAnnotation(annotationClass);
        return this;
    }

    /**
     * 导出 SQL 文件
     *
     * @param outputFile
     * @throws IOException
     */
    public void export(String outputFile, SQLGenerator generator) throws IOException {
        classes.addAll(entitySearcher.search());

        List<String> tableDSLList = classes.stream()
            .sorted(Comparator.comparing(Class::getSimpleName))
            .map(clazz -> {
                logger.trace("Processing class: {}", clazz.getName());
                TableSpec tableSpec = SpecMaker.makeTableSpec(clazz);
                try {
                    return generator.createTable(tableSpec);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (String dsl : tableDSLList) {
                fos.write(dsl.getBytes(StandardCharsets.UTF_8));
                fos.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}