package cc.ddrpa.dorian.elias;


import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import cc.ddrpa.dorian.elias.annotation.EliasTable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

public class EntityExtractor {

    /**
     * 找出所有被 {@link EliasTable} 注解标注的类
     *
     * @param packageName
     * @return
     */
    public static Set<Class<?>> findAll(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        Reflections reflections = new Reflections(
            new ConfigurationBuilder().forPackage(packageName));
        Set<Class<?>> annotated = reflections.get(
            SubTypes.of(TypesAnnotated.with(EliasTable.class)).asClass());
        annotated.forEach(clazz -> {
            EliasTable eliasTable = clazz.getAnnotation(EliasTable.class);
            if (Objects.isNull(eliasTable)) {
                // 如果 DTO 类继承了需要生成表的实体类，那么它会被反射找出来，但是获取这个注解时为 null
                // 不需要为这种 DTO 生成表
                return;
            }
            if (!eliasTable.enable()) {
                // 手动关闭表生成
                return;
            }
            classes.add(clazz);
        });
        return classes;
    }
}