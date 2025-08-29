package cc.ddrpa.dorian.elias.core;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 * Utility for discovering entity classes in specified packages using reflection.
 * 
 * <p>This class provides a fluent API for configuring package scanning to find classes
 * that should be converted to database tables. It supports both annotation-based discovery
 * (finding classes with specific annotations) and general {@link EliasTable} annotation scanning.
 * 
 * <p>Typical usage:
 * <pre>{@code
 * Set<Class<?>> entities = new EntitySearcher()
 *     .addPackage("com.example.model")
 *     .useAnnotation(Entity.class)
 *     .search();
 * }</pre>
 */
public class EntitySearcher {

    private final List<String> includePackages = new ArrayList<>(1);
    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Class<? extends Annotation>> annotationClasses = new HashSet<>();

    /**
     * Adds a package path for entity class discovery.
     * 
     * @param packageRef package name to scan (e.g., "com.example.model")
     * @return this searcher instance for method chaining
     */
    public EntitySearcher addPackage(String packageRef) {
        includePackages.add(packageRef);
        return this;
    }

    /**
     * Adds multiple package paths for entity class discovery.
     * 
     * @param packageRefs collection of package names to scan
     * @return this searcher instance for method chaining
     */
    public EntitySearcher addPackages(Collection<String> packageRefs) {
        includePackages.addAll(packageRefs);
        return this;
    }

    /**
     * Configures the searcher to find classes annotated with the specified annotation.
     * 
     * <p>Classes found must have the annotation present at runtime. This is in addition
     * to the default {@link EliasTable} annotation scanning.
     * 
     * @param annotationClass annotation class to search for (e.g., Entity.class)
     * @return this searcher instance for method chaining
     */
    public EntitySearcher useAnnotation(Class<? extends Annotation> annotationClass) {
        annotationClasses.add(annotationClass);
        return this;

    }

    /**
     * Executes the search and returns all discovered entity classes.
     * 
     * <p>Scans all configured packages for classes with {@link EliasTable} annotations
     * and any additional annotations specified via {@link #useAnnotation(Class)}.
     * Classes that inherit annotations but don't have them directly are filtered out.
     * 
     * @return set of discovered entity classes ready for schema generation
     */
    public Set<Class<?>> search() {
        for (String packageRef : includePackages) {
            find(packageRef);
        }
        if (!annotationClasses.isEmpty()) {
            for (String packageRef : includePackages) {
                for (Class<? extends Annotation> annotationClass : annotationClasses) {
                    find(packageRef, annotationClass);
                }
            }
        }
        return classes;
    }

    private <A extends Annotation> void find(String packageRef, Class<A> annotationClass) {
        Reflections reflections = new Reflections(
            new ConfigurationBuilder().forPackage(packageRef));
        Set<Class<?>> annotated = reflections.get(
            SubTypes.of(TypesAnnotated.with(annotationClass)).asClass());
        for (Class<?> clazz : annotated) {
            A requiredAnnotation = clazz.getAnnotation(annotationClass);
            if (Objects.isNull(requiredAnnotation)) {
                // 如果 DTO 类继承了需要生成表的实体类，那么它会被反射找出来，但是获取这个注解时为 null
                // 不需要为这种 DTO 生成表
                continue;
            }
            classes.add(clazz);
        }
    }

    private void find(String packageRef) {
        Reflections reflections = new Reflections(
            new ConfigurationBuilder().forPackage(packageRef));
        Set<Class<?>> annotated = reflections.get(
            SubTypes.of(TypesAnnotated.with(EliasTable.class)).asClass());
        for (Class<?> clazz : annotated) {
            EliasTable eliasTable = clazz.getAnnotation(EliasTable.class);
            if (Objects.isNull(eliasTable)) {
                // 如果 DTO 类继承了需要生成表的实体类，那么它会被反射找出来，但是获取这个注解时为 null
                // 不需要为这种 DTO 生成表
                continue;
            }
            classes.add(clazz);
        }
    }
}
