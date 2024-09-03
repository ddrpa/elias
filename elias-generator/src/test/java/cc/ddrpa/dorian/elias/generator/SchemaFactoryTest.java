package cc.ddrpa.dorian.elias.generator;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class SchemaFactoryTest {

    @Test
    void generateTest() throws ClassNotFoundException, IOException {
        new SchemaFactory()
            .dropIfExists(true)
            .addAllAnnotatedClass("cc.ddrpa.dorian")
            .addClass(Class.forName("cc.ddrpa.dorian.elias.playground.AccountSetting"))
            .export("./target/generateTest.sql");
    }
}