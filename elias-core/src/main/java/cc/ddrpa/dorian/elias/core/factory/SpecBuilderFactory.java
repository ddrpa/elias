package cc.ddrpa.dorian.elias.core.factory;

import static cc.ddrpa.dorian.elias.core.SpecUtils.getColumnName;

import cc.ddrpa.dorian.elias.core.annotation.DefaultValue;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.lang.reflect.Field;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory interface for creating database column specifications from Java fields.
 * 
 * <p>Implementations determine if they can handle a specific Java field type and provide
 * a {@link ColumnSpecBuilder} configured with appropriate database column properties.
 * The factory pattern allows extensible type mapping support.
 * 
 * <p>The default {@link #builder(Field)} method handles common annotation processing
 * including MyBatis-Plus annotations ({@code @TableId}, {@code @TableField}, {@code @TableLogic})
 * and validation annotations ({@code @NotNull}, etc.).
 * 
 * @see ColumnSpecBuilder
 */
public interface SpecBuilderFactory {

    Logger logger = LoggerFactory.getLogger(SpecBuilderFactory.class);

    /**
     * Determines if this factory can handle the specified field type.
     * 
     * @param fieldTypeName fully qualified name of the field's type
     * @param field the Java field being processed
     * @return true if this factory can create a column specification for this field
     */
    boolean fit(String fieldTypeName, Field field);

    /**
     * Creates a column specification builder for the given field.
     * 
     * <p>The default implementation handles common annotation processing:
     * <ul>
     * <li>Column naming from {@code @TableId} or {@code @TableField} annotations
     * <li>Primary key detection from {@code @TableId}
     * <li>Auto-increment detection from {@code @TableId} type
     * <li>Default values from {@code @TableLogic} and {@code @DefaultValue}
     * <li>Nullability from validation annotations ({@code @NotNull}, etc.)
     * </ul>
     * 
     * <p>Implementations should call this default method and then configure
     * type-specific properties like data type, length, and precision.
     * 
     * @param field the Java field to process
     * @return configured column specification builder
     */

    default ColumnSpecBuilder builder(Field field) {
        ColumnSpecBuilder builder = new ColumnSpecBuilder();
        // 设置 column 名称，优先级 TableId / TableField 声明 > 从 property 推导
        // NEED_CHECK 为增强可读性，暂时放弃部分性能，重复获取注解信息
        builder.setName(getColumnName(field));
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableField} 注解
         */
        if (field.isAnnotationPresent(TableField.class)) {
            TableField tableFieldAnnotation = field.getAnnotation(TableField.class);
            if (StringUtils.isNoneBlank(Objects.requireNonNull(tableFieldAnnotation).value())) {
                builder.setName(tableFieldAnnotation.value());
            }
        } else if (field.isAnnotationPresent(TableId.class)) {
            /**
             * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableId} 注解
             */
            TableId tableId = field.getAnnotation(TableId.class);
            if (StringUtils.isNoneBlank(Objects.requireNonNull(tableId).value())) {
                builder.setName(tableId.value());
            }
        }
        // 数据类型和长度精度等配置的判断交给子类
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableId} 注解，设置为主键
         */
        if (field.isAnnotationPresent(TableId.class)) {
            TableId tableId = Objects.requireNonNull(field.getAnnotation(TableId.class));
            // 设置主键时需要指定是否设置 autoIncrement 属性
            builder.setPrimaryKey(
                IdType.AUTO.equals(tableId.type()) || IdType.NONE.equals(tableId.type()));
            // 如果是主键，则暂时不用看其他配置了
            return builder;
        }
        /**
         * 如果字段有 {@link com.baomidou.mybatisplus.annotation.TableLogic} 注解，设置 defaultValue 为 0
         * <p>
         * NEED_CHECK 这一功能有待商榷，在 Mybatis-Plus 中 deleted-value 可以通过配置文件修改为其他值
         */
        if (field.isAnnotationPresent(TableLogic.class)) {
            builder.setDefaultValue("0");
        }
        /**
         * 如果字段有 {@link javax.validation.constraints.NotBlank},
         * {@link javax.validation.constraints.NotEmpty},
         * {@link javax.validation.constraints.NotNull} 注解，设置为非空
         */
        try {
            if (field.isAnnotationPresent(javax.validation.constraints.NotNull.class) ||
                field.isAnnotationPresent(javax.validation.constraints.NotEmpty.class) ||
                field.isAnnotationPresent(javax.validation.constraints.NotBlank.class)
            ) {
                builder.setNullable(false);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotNull.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotEmpty.class)
            ) {
                builder.setNullable(false);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        // DefaultValue 注解修饰的属性
        if (field.isAnnotationPresent(DefaultValue.class)) {
            DefaultValue defaultValue = Objects.requireNonNull(
                field.getAnnotation(DefaultValue.class));
            builder.setDefaultValue(defaultValue.value());
        }
        return builder;
    }
}