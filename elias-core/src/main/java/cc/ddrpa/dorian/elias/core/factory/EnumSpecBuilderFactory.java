package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import com.baomidou.mybatisplus.annotation.IEnum;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumSpecBuilderFactory implements SpecBuilderFactory {

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.getType().isEnum();
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        builder.setDataType("tinyint")
            .setLength(deriveDataLength(field.getType()));
        return builder;
    }

    /**
     * 根据枚举成员的数量和 getValue 方法推断需要的 byte 长度能够覆盖值范围
     *
     * @param fieldType
     * @return
     */
    protected Long deriveDataLength(Class<?> fieldType) {
        Object[] enumConstants = fieldType.getEnumConstants();
        int valueRange = enumConstants.length;
        /**
         * 检查是否实现了 {@link IEnum} 接口
         */
        boolean implementsIEnum = false;
        for (Type iface : fieldType.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) iface;
                if (pt.getRawType().getTypeName()
                    .equals("com.baomidou.mybatisplus.annotation.IEnum")) {
                    Type typeArg = pt.getActualTypeArguments()[0];
                    if (typeArg.getTypeName().equals("java.lang.Integer")) {
                        implementsIEnum = true;
                        break;
                    }
                }
            }
        }
        if (implementsIEnum) {
            // 如果实现了 IEnum 接口，返回 Integer 的长度
            try {
                valueRange = getValueRange(fieldType, enumConstants);
            } catch (Exception ignored) {
            }
        }
        return (long) Math.ceil(
            (Math.log(valueRange) / Math.log(2)) / 8
        );
    }

    /**
     * 对实现了 {@link IEnum} 接口的枚举类型，获取其成员 Value 值的范围
     *
     * @param fieldType
     * @param enumConstants
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    protected Integer getValueRange(Class<?> fieldType, Object[] enumConstants)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getValueMethod = fieldType.getMethod("getValue");
        List<Integer> values = new ArrayList<>();
        for (Object constant : enumConstants) {
            Integer value = (Integer) getValueMethod.invoke(constant);
            values.add(value);
        }
        int min = Collections.min(values);
        int max = Collections.max(values);
        return max - Math.min(0, min);
    }
}