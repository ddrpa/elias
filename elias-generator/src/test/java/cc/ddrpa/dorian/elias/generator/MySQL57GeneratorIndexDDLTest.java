package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class MySQL57GeneratorIndexDDLTest {

    @Test
    void shouldRenderCreateAndDropIndexSql() throws IOException {
        MySQL57Generator generator = new MySQL57Generator().setDropIfExists(false);
        IndexSpec indexSpec = new IndexSpec()
                .setName("uk_username_email")
                .setUnique(true)
                .setColumns("username ASC, email_address DESC");
        String createSql = generator.createIndex("tbl_account", indexSpec);
        String dropSql = generator.dropIndex("tbl_account", "uk_username_email");
        Assertions.assertTrue(createSql.contains(
                "create unique index uk_username_email on `tbl_account` (username ASC, email_address DESC);"));
        Assertions.assertTrue(dropSql.contains("drop index `uk_username_email` on `tbl_account`;"));
    }
}
