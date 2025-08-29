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


/**
 * Main entry point for generating SQL DDL from Java entity classes.
 * 
 * <p>This class provides a fluent API to configure entity discovery and generate complete
 * SQL database schemas. It combines {@link EntitySearcher} for class discovery with
 * {@link SpecMaker} for specification generation and {@link SQLGenerator} for final output.
 * 
 * <p>Typical usage:
 * <pre>{@code
 * new SchemaFactory()
 *     .addPackage("com.example.entities")
 *     .useAnnotation(Entity.class)
 *     .export("schema.sql", new MySQL57Generator());
 * }</pre>
 */
public class SchemaFactory {

    private final Logger logger = LoggerFactory.getLogger(SchemaFactory.class);
    private final Set<Class<?>> classes = new HashSet<>(5);
    private final EntitySearcher entitySearcher = new EntitySearcher();

    /**
     * Includes all eligible classes from the specified package in schema generation.
     * 
     * @param packageRef package name to scan (e.g., "com.example.entities")
     * @return this factory instance for method chaining
     */
    public SchemaFactory addPackage(String packageRef) {
        entitySearcher.addPackage(packageRef);
        return this;
    }

    /**
     * Includes a specific class in schema generation.
     * 
     * @param clazz the entity class to include
     * @param <T> class type parameter
     * @return this factory instance for method chaining
     */
    public <T> SchemaFactory addClass(Class<T> clazz) {
        classes.add(clazz);
        return this;
    }

    /**
     * Configures entity discovery to include classes annotated with the specified annotation.
     * 
     * <p>This is in addition to the default {@link cc.ddrpa.dorian.elias.core.annotation.EliasTable} scanning.
     * 
     * @param annotationClass annotation to search for (e.g., Entity.class, Table.class)
     * @param <A> annotation type parameter
     * @return this factory instance for method chaining
     */
    public <A extends Annotation> SchemaFactory useAnnotation(Class<A> annotationClass) {
        entitySearcher.useAnnotation(annotationClass);
        return this;
    }

    /**
     * Generates and exports SQL DDL to the specified file.
     * 
     * <p>Discovers all configured entity classes, converts them to table specifications,
     * and generates SQL CREATE TABLE statements using the provided generator. Classes
     * are processed in alphabetical order by simple name.
     * 
     * @param outputFile path to the output SQL file
     * @param generator SQL generator implementation (e.g., MySQL57Generator)
     * @throws IOException if file writing fails
     * @throws RuntimeException if table specification generation fails
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