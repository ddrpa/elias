package cc.ddrpa.dorian.elias.generator;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SchemaFactoryTest {

    @Test
    void generateTest() throws IOException {
        SQLGenerator generator = new MySQL57Generator()
                .enableH2Compatibility()
                .setDropIfExists(false);
        new SchemaFactory()
                .addPackage("cc.ddrpa.dorian")
                .useAnnotation(TableName.class)
                .export("./target/generateTest.sql", generator);
    }
}