package cc.ddrpa.dorian.elias.core;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
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

    public EntitySearcher addPackage(String packageRef) {
        includePackages.add(packageRef);
        return this;
    }

    public EntitySearcher addPackages(Collection<String> packageRefs) {
        includePackages.addAll(packageRefs);
        return this;
    }

    public Set<Class<?>> search() {
        for (String packageRef : includePackages) {
            find(packageRef);
        }
        return classes;
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
