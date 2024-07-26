package cc.ddrpa.dorian.elias.generator;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class MySQLSchemaGeneratorTest {

    @Test
    void generateTest() throws ClassNotFoundException, IOException {
        new MySQLSchemaGenerator()
            .enableDropIfExists()
            .addAllAnnotatedClass("cc.ddrpa.dorian")
            .addClass(Class.forName("cc.ddrpa.dorian.playground.AccountSetting"))
            .export("./target/generateTest.sql");
    }
}