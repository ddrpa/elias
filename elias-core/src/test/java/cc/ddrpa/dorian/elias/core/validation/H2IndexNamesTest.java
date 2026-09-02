package cc.ddrpa.dorian.elias.core.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class H2IndexNamesTest {

    @Test
    void detectsSyntheticAlias() {
        Assertions.assertTrue(H2IndexNames.isSyntheticAlias("uk_gdt_account_id", "uk_gdt_account_id_INDEX_2"));
        Assertions.assertTrue(H2IndexNames.isSyntheticAlias("uk_foo", "UK_FOO_INDEX_E"));
        Assertions.assertFalse(H2IndexNames.isSyntheticAlias("uk_foo", "uk_foo"));
        Assertions.assertFalse(H2IndexNames.isSyntheticAlias("uk_foo", "uk_foo_extra"));
        Assertions.assertFalse(H2IndexNames.isSyntheticAlias("uk_foo", "uk_bar_INDEX_2"));
    }
}
