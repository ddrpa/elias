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

public class EntitySearcher {

    private final List<String> includePackages = new ArrayList<>(1);
    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Class<? extends Annotation>> annotationClasses = new HashSet<>();

    /**
     * 添加实体类搜索路径
     *
     * @param packageRef
     * @return
     */
    public EntitySearcher addPackage(String packageRef) {
        includePackages.add(packageRef);
        return this;
    }

    /**
     * 添加多个实体类搜索路径
     *
     * @param packageRefs
     * @return
     */
    public EntitySearcher addPackages(Collection<String> packageRefs) {
        includePackages.addAll(packageRefs);
        return this;
    }

    /**
     * 要求 Elias 搜索使用该注解修饰的类
     *
     * @param annotationClass
     * @return
     */
    public EntitySearcher useAnnotation(Class<? extends Annotation> annotationClass) {
        annotationClasses.add(annotationClass);
        return this;

    }

    /**
     * 查找类
     *
     * @return
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
