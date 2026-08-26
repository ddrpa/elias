package cc.ddrpa.dorian.elias.core;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.Index;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.annotation.UniqueIndex;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;

class SpecMakerIndexTest {

    @Test
    void shouldBuildIndexesFromTableAndFieldAnnotations() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(FieldAndTableIndexEntity.class);
        Assertions.assertEquals(4, tableSpec.getIndexes().size());
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> "idx_username".equals(index.getName())
                        && "username DESC".equals(index.getColumns())));
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> "uk_email".equals(index.getName())
                        && index.isUnique()
                        && "email_address ASC".equals(index.getColumns())));
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> "idx_email_address".equals(index.getName())
                        && "email_address DESC".equals(index.getColumns())));
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> "idx_username_email_address".equals(index.getName())
                        && "username, email_address".equals(index.getColumns())));
    }

    @Test
    void shouldAllowNullableColumnInUniqueCompositeIndex() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(UniqueCompositeWithNullableEntity.class);
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> index.isUnique()
                        && "username, create_time".equals(index.getColumns())));
    }

    @Test
    void shouldBuildGroupedIndexByPositionThenColumnName() {
        TableSpec tableSpec = SpecMaker.makeTableSpec(DuplicatePositionGroupedEntity.class);
        Assertions.assertTrue(tableSpec.getIndexes().stream()
                .anyMatch(index -> "email_address ASC, username DESC".equals(index.getColumns())));
    }

    @EliasTable(indexes = {
            @EliasTable.Index(columns = "username, email_address")
    })
    private static class FieldAndTableIndexEntity {
        @Index(desc = true)
        @NotNull
        private String username;

        @TableField("email_address")
        @UniqueIndex(name = "uk_email")
        @Index(desc = true)
        @NotNull
        private String email;
    }

    @EliasTable(indexes = {
            @EliasTable.Index(columns = "username, create_time", unique = true)
    })
    @SuppressWarnings("unused")
    private static class UniqueCompositeWithNullableEntity {
        @NotNull
        private String username;
        private String createTime;
    }

    @EliasTable
    @SuppressWarnings("unused")
    private static class DuplicatePositionGroupedEntity {
        @TableField("email_address")
        @Index(group = "group_dup", name = "idx_group_dup", pos = 1)
        @NotNull
        private String email;

        @Index(group = "group_dup", pos = 1, desc = true)
        @NotNull
        private String username;
    }
}
