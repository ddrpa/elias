package cc.ddrpa.dorian.elias.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.Optional;

class OptionalAnnotationAttributesTest {

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
