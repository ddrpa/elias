package cc.ddrpa.dorian.elias.core;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 通过 FQCN 反射读取可选第三方注解属性，避免为可选生态引入 compile 依赖。
 */
public final class OptionalAnnotationAttributes {

    private OptionalAnnotationAttributes() {
    }

    /**
     * 判断元素上是否存在给定 FQCN 的注解（注解类不在 classpath 时视为不存在）。
     */
    public static boolean isPresent(AnnotatedElement element, String annotationClassName) {
        return findAnnotation(element, annotationClassName).isPresent();
    }

    /**
     * 读取注解上的字符串属性；注解不存在、属性缺失或值为空白时返回 empty。
     */
    public static Optional<String> readStringAttribute(AnnotatedElement element,
                                                       String annotationClassName,
                                                       String attributeName) {
        return findAnnotation(element, annotationClassName).flatMap(annotation -> {
            try {
                Method method = annotation.annotationType().getMethod(attributeName);
                Object value = method.invoke(annotation);
                if (value instanceof String text && StringUtils.isNotBlank(text)) {
                    return Optional.of(text);
                }
                return Optional.empty();
            } catch (ReflectiveOperationException ignored) {
                return Optional.empty();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Optional<Annotation> findAnnotation(AnnotatedElement element,
                                                       String annotationClassName) {
        try {
            Class<?> raw = Class.forName(annotationClassName);
            if (!Annotation.class.isAssignableFrom(raw)) {
                return Optional.empty();
            }
            Annotation annotation = element.getAnnotation((Class<? extends Annotation>) raw);
            return Optional.ofNullable(annotation);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }
}
