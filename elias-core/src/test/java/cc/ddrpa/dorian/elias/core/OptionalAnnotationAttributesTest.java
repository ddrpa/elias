package cc.ddrpa.dorian.elias.core;

import cc.ddrpa.dorian.elias.core.clfixture.JakartaNotNullEntity;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class OptionalAnnotationAttributesTest {

    private static final String JAKARTA_NOT_NULL = "jakarta.validation.constraints.NotNull";

    @Test
    void shouldReadStringAttributeFromPresentAnnotation() throws Exception {
        Field field = Sample.class.getDeclaredField("named");
        Optional<String> value = OptionalAnnotationAttributes.readStringAttribute(
                field, Desc.class.getName(), "value");
        Assertions.assertEquals(Optional.of("hello"), value);
    }

    @Test
    void shouldReturnEmptyWhenAnnotationMissing() throws Exception {
        Field field = Sample.class.getDeclaredField("plain");
        Assertions.assertTrue(OptionalAnnotationAttributes.readStringAttribute(
                field, Desc.class.getName(), "value").isEmpty());
        Assertions.assertFalse(OptionalAnnotationAttributes.isPresent(field, Desc.class.getName()));
    }

    @Test
    void shouldReturnEmptyForUnknownAnnotationClass() throws Exception {
        Field field = Sample.class.getDeclaredField("named");
        Assertions.assertTrue(OptionalAnnotationAttributes.readStringAttribute(
                field, "io.swagger.v3.oas.annotations.media.Schema", "description").isEmpty());
    }

    @Test
    void shouldDetectJakartaNotNullLoadedByChildClassLoader() throws Exception {
        Class<?> entityClass = loadFixtureInChildClassLoader();
        Field field = entityClass.getDeclaredField("departName");

        Class<?> annotationOnField = field.getDeclaredAnnotations()[0].annotationType();
        Assertions.assertEquals(JAKARTA_NOT_NULL, annotationOnField.getName());
        Assertions.assertNotSame(
                jakarta.validation.constraints.NotNull.class,
                annotationOnField,
                "child CL must load a distinct NotNull Class instance");

        // Parent-CL Class.forName + getAnnotation(Class) would fail here; FQCN match must succeed.
        Assertions.assertNull(
                field.getAnnotation(jakarta.validation.constraints.NotNull.class),
                "getAnnotation with parent NotNull Class must miss child annotation");
        Assertions.assertTrue(
                OptionalAnnotationAttributes.isPresent(field, JAKARTA_NOT_NULL),
                "FQCN match must see @NotNull from a different ClassLoader");

        TableSpec tableSpec = SpecMaker.makeTableSpec(entityClass);
        ColumnSpec column = tableSpec.getColumns().stream()
                .filter(c -> "depart_name".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        Assertions.assertFalse(column.isNullable(),
                "SpecMaker must treat child-CL @NotNull as NOT NULL");
    }

    /**
     * Child-first loader for the fixture package and {@code jakarta.validation.*}, mimicking
     * changelog-export's project URLClassLoader vs plugin ClassLoader split.
     * The loader is intentionally not closed so loaded classes remain usable for the test.
     */
    private static Class<?> loadFixtureInChildClassLoader() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(codeSourceUrl(JakartaNotNullEntity.class));
        urls.add(codeSourceUrl(jakarta.validation.constraints.NotNull.class));

        ClassLoader parent = OptionalAnnotationAttributes.class.getClassLoader();
        URLClassLoader child = new URLClassLoader(urls.toArray(URL[]::new), parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("cc.ddrpa.dorian.elias.core.clfixture")
                        || name.startsWith("jakarta.validation")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            loaded = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(loaded);
                        }
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
        Class<?> entityClass = Class.forName(JakartaNotNullEntity.class.getName(), true, child);
        Assertions.assertNotSame(JakartaNotNullEntity.class, entityClass);
        return entityClass;
    }

    private static URL codeSourceUrl(Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            throw new IllegalStateException("Cannot locate code source for " + type.getName());
        }
        return source.getLocation();
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Desc {
        String value();
    }

    @SuppressWarnings("unused")
    private static class Sample {
        @Desc("hello")
        private String named;
        private String plain;
    }
}
