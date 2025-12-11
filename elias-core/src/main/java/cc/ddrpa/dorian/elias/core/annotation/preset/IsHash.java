package cc.ddrpa.dorian.elias.core.annotation.preset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsHash {

    /**
     * 指定 Hash 算法类型，默认为 xxHash64
     */
    HashType value() default HashType.XX_HASH64;

    enum HashType {
        /**
         * MD5 - 128 bits (16 bytes)
         */
        MD5(16),
        /**
         * SHA-1 - 160 bits (20 bytes)
         */
        SHA1(20),
        /**
         * SHA-256 - 256 bits (32 bytes)
         */
        SHA256(32),
        /**
         * SHA-384 - 384 bits (48 bytes)
         */
        SHA384(48),
        /**
         * SHA-512 - 512 bits (64 bytes)
         */
        SHA512(64),
        /**
         * xxHash64 - 64 bits (8 bytes)
         */
        XX_HASH64(8),
        /**
         * MurmurHash3 128-bit - 128 bits (16 bytes)
         */
        MURMUR3_128(16),
        /**
         * BLAKE2b-256 - 256 bits (32 bytes)
         */
        BLAKE2B_256(32),
        /**
         * BLAKE2b-512 - 512 bits (64 bytes)
         */
        BLAKE2B_512(64);

        private final int length;

        HashType(int length) {
            this.length = length;
        }

        /**
         * 获取该 Hash 算法对应的二进制存储长度（字节数）
         *
         * @return BINARY 列的长度
         */
        public int getLength() {
            return length;
        }
    }
}