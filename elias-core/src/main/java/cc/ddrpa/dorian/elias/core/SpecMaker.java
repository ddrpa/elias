package cc.ddrpa.dorian.elias.core;

import static org.reflections.ReflectionUtils.Fields;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.TypeOverride;
import cc.ddrpa.dorian.elias.core.factory.BigDecimalSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.BinarySpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.BlobSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.BooleanSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.CharSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.DateTimeSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.EnumSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.FloatSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.GeometrySpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.IntegerSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.SpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.TextSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.factory.TypeOverrideSpecBuilderFactory;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.SpatialIndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecMaker {

    private static final Logger logger = LoggerFactory.getLogger(SpecMaker.class);
    /**
     * 预设的 SpecBuilderFactory 列表，参考 <a
     * href="https://docs.oracle.com/cd/E19501-01/819-3659/gcmaz/">Supported Data Types</a> 实现，
     * {@link TypeOverride} 注解的优先级高于其他判断，其他类型推断优先级见 factory 实例的注册顺序
     */
    private static final List<SpecBuilderFactory> factories = List.of(
        new TypeOverrideSpecBuilderFactory(),
        new TextSpecBuilderFactory(),
        new IntegerSpecBuilderFactory(),
        new DateTimeSpecBuilderFactory(),
        new EnumSpecBuilderFactory(),
        new FloatSpecBuilderFactory(),
        new BooleanSpecBuilderFactory(),
        new BigDecimalSpecBuilderFactory(),
        new BinarySpecBuilderFactory(),
        new BlobSpecBuilderFactory(),
        new CharSpecBuilderFactory(),
        new GeometrySpecBuilderFactory()
    );

    /**
     * 将 Java 类转换为 TableSpec
     *
     * @param clazz
     * @return
     */
    public static TableSpec makeTableSpec(Class<?> clazz) {
        TableSpec tableSpec = new TableSpec();
        tableSpec.setName(SpecUtils.getTableName(clazz));
        // 处理类成员
        Set<Field> fields = ReflectionUtils.get(Fields.of(clazz));
        // java.io.Serial 在 Java 11 中不可用，且该注解不会在运行时出现，无法用于判断是否忽略
        List<ColumnSpec> columns = fields.stream()
            .filter(SpecUtils::shouldIgnoreColumn)
            .map(f -> {
                Class<?> fClazz = f.getDeclaringClass();
                int depth = 0;
                while (fClazz != null) {
                    depth++;
                    fClazz = fClazz.getSuperclass();
                }
                return Pair.of(depth, f);
            })
            .sorted(Comparator.comparingInt(Pair::getLeft))
            .map(Pair::getRight)
            .map(SpecMaker::processField)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (columns.stream().filter(ColumnSpec::isPrimaryKey).count() > 1) {
            throw new IllegalStateException(
                "Multiple primary keys found in class: " + clazz.getName());
        }
        tableSpec.setColumns(columns);
        EliasTable eliasTableAnnotation = clazz.getAnnotation(EliasTable.class);
        if (eliasTableAnnotation != null && eliasTableAnnotation.indexes().length > 0) {
            List<IndexSpec> indexSpecs = createIndexSpecs(eliasTableAnnotation, columns);
            tableSpec.setIndexes(indexSpecs);
            List<SpatialIndexSpec> spatialIndexSpecs = createSpatialIndexSpecs(eliasTableAnnotation,
                columns);
            tableSpec.setSpatialIndexSpecs(spatialIndexSpecs);
        }
        return tableSpec;
    }

    /**
     * 解析索引配置
     *
     * @param eliasTableAnno
     * @param columnSpecs
     * @return
     */
    protected static List<IndexSpec> createIndexSpecs(EliasTable eliasTableAnno,
        List<ColumnSpec> columnSpecs) {
        Set<String> existedColumnNameSet = columnSpecs.stream()
            .map(ColumnSpec::getName)
            .collect(Collectors.toSet());
        Set<String> nonNullColumnNameSet = columnSpecs.stream()
            .filter(columnSpec -> !columnSpec.isNullable())
            .map(ColumnSpec::getName)
            .collect(Collectors.toSet());
        Map<String, IndexSpec> indexSpecMap = Arrays.stream(eliasTableAnno.indexes())
            .map(indexAnno -> {
                String indexName;
                String columnList = indexAnno.columns();
                if (StringUtils.isBlank(columnList)) {
                    throw new IllegalStateException(
                        "Index column list is empty, please specify columns in @EliasTable.indexes");
                }
                if (StringUtils.isNoneBlank(indexAnno.name())) {
                    indexName = indexAnno.name();
                } else {
                    List<String> annotatedColumns = Arrays.stream(
                            indexAnno.columns().split(","))
                        .map(columnSpec -> columnSpec.trim().split(" ")[0])
                        .toList();
                    if (indexAnno.unique()) {
                        indexName = "uk_";
                        // 唯一索引成员必须满足 not_null 条件
                        if (annotatedColumns.stream()
                            .anyMatch(c -> !nonNullColumnNameSet.contains(c))) {
                            throw new IllegalStateException(
                                "Annotated column not found or is nullable");
                        }
                    } else {
                        indexName = "idx_";
                        if (annotatedColumns.stream()
                            .anyMatch(c -> !existedColumnNameSet.contains(c))) {
                            throw new IllegalStateException("Annotated column not found");
                        }
                    }
                    indexName += String.join("_", annotatedColumns);
                }
                if (indexName.length() > 64) {
                    // 索引名称超过 64 个字符截断，可能会导致重名
                    indexName = indexName.substring(0, 64);
                }
                return new IndexSpec()
                    .setName(indexName)
                    .setUnique(indexAnno.unique())
                    .setColumns(indexAnno.columns());
            })
            .collect(Collectors.toMap(IndexSpec::getName, spec -> spec));
        if (eliasTableAnno.indexes().length != indexSpecMap.size()) {
            throw new IllegalStateException(
                "Duplicate index names found in EliasTable annotation, some indexes may be ignored.");
        }
        return new ArrayList<>(indexSpecMap.values());
    }

    /**
     * 解析空间索引配置
     *
     * @param eliasTableAnno
     * @param columnSpecs
     * @return
     */
    protected static List<SpatialIndexSpec> createSpatialIndexSpecs(
        EliasTable eliasTableAnno, List<ColumnSpec> columnSpecs) {
        Map<String, SpatialIndexSpec> indexSpecMap = new HashMap<>();
        if (eliasTableAnno.autoSpatialIndexForGeometry()) {
            // 为所有非空的空间数据类型的列创建空间索引
            columnSpecs.stream()
                .filter(ColumnSpec::isGeometry)
                .filter(columnSpec -> !columnSpec.isNullable())
                .map(spec -> new SpatialIndexSpec()
                    .setName("sp_idx_" + spec.getName())
                    .setColumns(spec.getName()))
                .forEach(indexSpec -> indexSpecMap.put(indexSpec.getName(), indexSpec));
        }
        // 处理 @EliasTable.spatialIndexes 注解，参与索引的列必须为空间数据类型且 not null
        Set<String> existedColumnNameSet = columnSpecs.stream()
            .filter(ColumnSpec::isGeometry)
            .filter(columnSpec -> !columnSpec.isNullable())
            .map(ColumnSpec::getName)
            .collect(Collectors.toSet());
        Arrays.stream(eliasTableAnno.spatialIndexes())
            .map(indexAnno -> {
                String indexName;
                String columnList = indexAnno.columns();
                if (StringUtils.isBlank(columnList)) {
                    throw new IllegalStateException(
                        "Spatial index column list is empty, please specify columns in @EliasTable.spatialIndexes");
                }
                if (StringUtils.isNoneBlank(indexAnno.name())) {
                    indexName = indexAnno.name();
                } else {
                    indexName = "sp_idx_";
                    List<String> annotatedColumns = Arrays.stream(
                            indexAnno.columns().split(","))
                        .map(columnSpec -> columnSpec.trim().split(" ")[0])
                        .collect(Collectors.toList());
                    if (annotatedColumns.stream()
                        .anyMatch(c -> !existedColumnNameSet.contains(c))) {
                        throw new IllegalStateException(
                            "Annotated column with geo type not found in table or is nullable");
                    }
                    indexName += String.join("_", annotatedColumns);
                }
                if (indexName.length() > 64) {
                    // 索引名称超过 64 个字符截断，可能会导致重名
                    indexName = indexName.substring(0, 64);
                }
                return new SpatialIndexSpec()
                    .setName(indexName)
                    .setColumns(indexAnno.columns());
            })
            .forEach(
                spatialIndexSpec -> indexSpecMap.put(spatialIndexSpec.getName(), spatialIndexSpec));
        return new ArrayList<>(indexSpecMap.values());
    }

    /**
     * 将类的属性转换为列定义
     *
     * @param field
     * @return
     */
    private static ColumnSpec processField(Field field) {
        logger.trace("process field: {}", field.getName());
        String fieldTypeName = field.getType().getName();
        ColumnSpecBuilder builder = factories.stream().
            filter(f -> f.fit(fieldTypeName, field))
            .findFirst()
            .get().builder(field);
        return builder.build();
    }
}