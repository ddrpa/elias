package cc.ddrpa.dorian.elias.generator;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class SchemaFactoryTest {

    @Test
    void generateTest() throws ClassNotFoundException, IOException {
        new SchemaFactory()
            .dropIfExists(true)
            .addPackage("cc.ddrpa.dorian")
            .useAnnotation(TableName.class)
            .export("./target/generateTest.sql");
    }
}