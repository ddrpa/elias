package cc.ddrpa.dorian.elias.generator;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class MySQLSchemaGeneratorTest {

    @Test
    void generateTest() throws ClassNotFoundException, IOException {
        new MySQLSchemaGenerator("cc.ddrpa.dorian")
            .enableDropIfExists()
            .addAllAnnotatedClass()
//            .addClass(Class.forName("cc.ddrpa.dorian.playground.AccountSetting"))
            .setOutputFile("./target/generateTest.sql");
    }
}