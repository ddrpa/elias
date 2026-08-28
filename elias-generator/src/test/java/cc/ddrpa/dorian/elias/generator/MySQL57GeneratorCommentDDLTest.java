package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class MySQL57GeneratorCommentDDLTest {

    @Test
    void shouldRenderCommentOnCreateAddAndModify() throws IOException {
        MySQL57Generator generator = new MySQL57Generator().setDropIfExists(false);
        ColumnSpec column = new ColumnSpec()
                .setName("username")
                .setDataType("varchar")
                .setLength(64L)
                .setNullable(false)
                .setComment("login name");
        TableSpec table = new TableSpec()
                .setName("tbl_account")
                .setColumns(List.of(column));

        String createSql = generator.createTable(table);
        Assertions.assertTrue(createSql.contains("comment 'login name'"));

        String addSql = generator.addColumn("tbl_account", column);
        Assertions.assertTrue(addSql.contains("comment 'login name'"));

        ColumnModifySpec modifySpec = new ColumnModifySpec()
                .setColumnType("varchar(64)")
                .setNullable(false)
                .setComment("login name");
        String modifySql = generator.modifyColumn("tbl_account", "username", modifySpec);
        Assertions.assertTrue(modifySql.contains("comment 'login name'"));
    }

    @Test
    void shouldEscapeSingleQuotesInComment() throws IOException {
        MySQL57Generator generator = new MySQL57Generator().setDropIfExists(false);
        ColumnSpec column = new ColumnSpec()
                .setName("note")
                .setDataType("varchar")
                .setLength(32L)
                .setComment("O'Reilly");
        String addSql = generator.addColumn("tbl_account", column);
        Assertions.assertTrue(addSql.contains("comment 'O''Reilly'"),
                "actual SQL was: " + addSql);
    }

    @Test
    void shouldStripSemicolonsFromCommentInDdl() throws IOException {
        MySQL57Generator generator = new MySQL57Generator().setDropIfExists(false);
        String modifySql = generator.modifyColumn("stat_quality_dimension_daily", "dimension_id",
                new ColumnModifySpec()
                        .setColumnType("bigint(20)")
                        .setNullable(false)
                        .setComment("0=租户总分;1=分项"));
        Assertions.assertTrue(modifySql.contains("comment '0=租户总分 1=分项'"));
        Assertions.assertEquals(1,
                Arrays.stream(modifySql.split(";")).filter(s -> !s.isBlank()).count());
    }
}
