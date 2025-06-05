package cc.ddrpa.dorian.elias.core.spec;

public class ConstantsPool {

    // VARCHAR
    public static final long VARCHAR_DEFAULT_CHARACTER_LENGTH = 255L;
    public static final long VARCHAR_MAX_CHARACTER_LENGTH = 5000L;
    // TEXT
    public static final long TINYTEXT_MAX_CHARACTER_LENGTH = 255L;
    public static final long TEXT_MAX_CHARACTER_LENGTH = 65535L;
    public static final long MEDIUMTEXT_MAX_CHARACTER_LENGTH = 16_777_215L;
    public static final long LONGTEXT_MAX_CHARACTER_LENGTH = 4_294_967_295L;
    // BIG_DECIMAL，99999999.99 - −99,999,999.99
    public static final int BIG_DECIMAL_DEFAULT_PRECISION = 10;
    public static final int BIG_DECIMAL_DEFAULT_SCALE = 2;
}