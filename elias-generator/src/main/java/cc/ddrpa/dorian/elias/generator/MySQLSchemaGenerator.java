package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.EntityExtractor;
import cc.ddrpa.dorian.elias.spec.SpecMaker;
import cc.ddrpa.dorian.elias.spec.TableSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MySQLSchemaGenerator {

    private final Logger logger = LoggerFactory.getLogger(MySQLSchemaGenerator.class);
    private final Set<Class<?>> classes = new HashSet<>(5);
    private final Template template;

    private String database;
    private boolean dropIfExists = false;

    public MySQLSchemaGenerator() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class",
            ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        template = velocityEngine.getTemplate("mysql.vm");
    }

    private String ddl(TableSpec tableSpec) {
        VelocityContext context = new VelocityContext();
        context.put("t", tableSpec);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public MySQLSchemaGenerator enableDropIfExists() {
        this.dropIfExists = true;
        return this;
    }

    public MySQLSchemaGenerator setDatabase(String database) {
        this.database = database;
        return this;
    }

    /**
     * 添加指定包下的所有带有 EliasTable 注解的类
     *
     * @param packageName
     * @return
     */
    public MySQLSchemaGenerator addAllAnnotatedClass(String packageName) {
        Set<Class<?>> classesUnderGivenPackage = EntityExtractor.findAll(packageName);
        classes.addAll(classesUnderGivenPackage);
        classesUnderGivenPackage.forEach(
            clazz -> logger.info("Found annotated class: {}", clazz.getName()));
        return this;
    }

    /**
     * 添加一个特定的类
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> MySQLSchemaGenerator addClass(Class<T> clazz) {
        classes.add(clazz);
        logger.info("Added class: {}", clazz.getName());
        return this;
    }

    /**
     * 导出 SQL 文件
     *
     * @param outputFile
     * @throws IOException
     */
    public void export(String outputFile) throws IOException {
        List<String> tableDSLList = classes.stream()
            .sorted(Comparator.comparing(Class::getSimpleName))
            .map(clazz -> {
                logger.info("Processing class: {}", clazz.getName());
                TableSpec tableSpec = SpecMaker.makeTableSpec(clazz);
                return ddl(tableSpec);
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