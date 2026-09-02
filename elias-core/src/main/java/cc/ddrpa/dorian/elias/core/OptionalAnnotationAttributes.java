package cc.ddrpa.dorian.elias.core;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 通过 FQCN 读取可选第三方注解属性，避免为可选生态引入 compile 依赖。
 * <p>
 * 按注解类型名字符串匹配，不依赖 {@link Class#forName(String)} / {@link Class} 身份，
 * 以便 Maven 插件子 ClassLoader 加载的实体字段注解仍可被识别。
 */
public final class OptionalAnnotationAttributes {

    private OptionalAnnotationAttributes() {
    }

    /**
     * 判断元素上是否存在给定 FQCN 的注解（元素上无该注解时视为不存在）。
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

    private static Optional<Annotation> findAnnotation(AnnotatedElement element,
                                                       String annotationClassName) {
        if (element == null || annotationClassName == null || annotationClassName.isBlank()) {
            return Optional.empty();
        }
        for (Annotation annotation : element.getAnnotations()) {
            if (annotationClassName.equals(annotation.annotationType().getName())) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }
}
