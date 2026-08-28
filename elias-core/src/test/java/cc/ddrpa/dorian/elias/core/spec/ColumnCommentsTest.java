package cc.ddrpa.dorian.elias.core.spec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ColumnCommentsTest {

    @Test
    void shouldRemoveSemicolonsAndNewlines() {
        Assertions.assertEquals("0=租户总分 1=分项",
                ColumnComments.sanitize("0=租户总分;1=分项"));
        Assertions.assertEquals("line1 line2",
                ColumnComments.sanitize("line1\nline2"));
    }

    @Test
    void shouldEscapeQuotesForSqlLiteral() {
        Assertions.assertEquals("O''Reilly",
                ColumnComments.forSqlLiteral("O'Reilly"));
        Assertions.assertEquals("0=租户总分 1=分项",
                ColumnComments.forSqlLiteral("0=租户总分;1=分项"));
    }

    @Test
    void shouldReturnNullForBlankAfterSanitize() {
        Assertions.assertNull(ColumnComments.sanitize(";;;"));
        Assertions.assertNull(ColumnComments.forSqlLiteral("  \n  "));
    }
}
