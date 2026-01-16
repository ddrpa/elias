package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MySQL 5.7.5 之后支持的地理空间数据类型，见
 * <a href="https://dev.mysql.com/doc/refman/5.7/en/spatial-type-overview.html">11.4.1 Spatial Data
 * Types</a>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsEncrypted {

    /**
     * 明文最大长度，使用 AES-GCM / SM4-GCM 加密时，密文长度 = UTF-8 明文字节数 + 28（12 IV + 16 Tag），
     * 即使用 VARBINARY(4 * LENGTH + 28)
     *
     *
     * @return
     */
    int length() default 255;
}
