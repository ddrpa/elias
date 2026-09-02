package cc.ddrpa.dorian.elias.core.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * H2 {@code MODE=MySQL} renames inline {@code UNIQUE KEY name (...)} to {@code name_INDEX_*} in
 * JDBC metadata. Detect those synthetic aliases so export/validation can treat them as the
 * expected index name.
 */
public final class H2IndexNames {

    private static final Pattern SYNTHETIC_SUFFIX = Pattern.compile(
            "(?i)^(.+)_INDEX_[0-9A-Za-z]+$");

    private H2IndexNames() {
    }

    /**
     * Whether {@code actualName} is H2's synthetic form of {@code expectedName}
     * (e.g. {@code uk_foo} vs {@code uk_foo_INDEX_2}).
     */
    public static boolean isSyntheticAlias(String expectedName, String actualName) {
        if (expectedName == null || actualName == null
                || expectedName.isBlank() || actualName.isBlank()) {
            return false;
        }
        String expected = expectedName.toLowerCase(Locale.ROOT);
        String actual = actualName.toLowerCase(Locale.ROOT);
        if (expected.equals(actual)) {
            return false;
        }
        var matcher = SYNTHETIC_SUFFIX.matcher(actual);
        return matcher.matches() && expected.equals(matcher.group(1));
    }
}
