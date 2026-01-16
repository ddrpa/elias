package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.annotation.preset.IsEncrypted;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

public class EncryptedSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(IsEncrypted.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        IsEncrypted isEncryptedAnno = Objects.requireNonNull(field.getAnnotation(IsEncrypted.class));
        int plainTextLength = isEncryptedAnno.length();
        int cipherTextLength = plainTextLength * 4 + 28; // AES-GCM / SM4-GCM 加密后长度计算
        return SpecBuilderFactory.super.builder(field).setDataType("varbinary")
                .setLength(cipherTextLength);
    }
}