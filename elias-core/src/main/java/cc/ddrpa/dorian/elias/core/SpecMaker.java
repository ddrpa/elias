package cc.ddrpa.dorian.elias.core;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.Index;
import cc.ddrpa.dorian.elias.core.annotation.TypeOverride;
import cc.ddrpa.dorian.elias.core.annotation.UniqueIndex;
import cc.ddrpa.dorian.elias.core.factory.*;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.SpatialIndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.Fields;

public class SpecMaker {

    private static final Logger logger = LoggerFactory.getLogger(SpecMaker.class);
    /**
     * 预设的 SpecBuilderFactory 列表，参考 <a
     * href="https://docs.oracle.com/cd/E19501-01/819-3659/gcmaz/">Supported Data Types</a> 实现，
     * {@link TypeOverride} 注解的优先级高于其他判断，其他类型推断优先级见 factory 实例的注册顺序
     */
    private static final List<SpecBuilderFactory> factories = List.of(
            new TypeOverrideSpecBuilderFactory(),
            new EncryptedSpecBuilderFactory(),
            // 语义化注解 factory（依据注解判定，优先于按 Java 类型判定的 factory）
            new EmailSpecBuilderFactory(),
            new PhoneSpecBuilderFactory(),
            new UrlSpecBuilderFactory(),
            new MimeTypeSpecBuilderFactory(),
            new MoneySpecBuilderFactory(),
            new PercentageSpecBuilderFactory(),
            new JsonSpecBuilderFactory(),
            new IpSpecBuilderFactory(),
            new MacAddressSpecBuilderFactory(),
            // @Auto 在所有显式语义化注解之后、原生类型 factory 之前
            new AutoSpecBuilderFactory(),
            // 原生类型 factory
            new TextSpecBuilderFactory(),
            new IntegerSpecBuilderFactory(),
            new DateTimeSpecBuilderFactory(),
            new EnumSpecBuilderFactory(),
            new FloatSpecBuilderFactory(),
            new BooleanSpecBuilderFactory(),
            new BigDecimalSpecBuilderFactory(),
            new InetAddressSpecBuilderFactory(),
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
        if (eliasTableAnnotation != null) {
            List<IndexSpec> indexSpecs = createIndexSpecs(eliasTableAnnotation, columns, fields);
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
                                                      List<ColumnSpec> columnSpecs,
                                                      Set<Field> fields) {
        Set<String> existedColumnNameSet = columnSpecs.stream()
                .map(ColumnSpec::getName)
                .collect(Collectors.toSet());
        Map<String, IndexSpec> indexSpecMap = new LinkedHashMap<>();
        List<IndexSpec> annotatedIndexSpecs = Arrays.stream(eliasTableAnno.indexes())
                .map(indexAnno -> new IndexSpec()
                        .setName(indexAnno.name())
                        .setUnique(indexAnno.unique())
                        .setColumns(indexAnno.columns()))
                .toList();
        for (IndexSpec indexSpec : annotatedIndexSpecs) {
            putIndexSpec(indexSpecMap, buildAndValidateIndexSpec(indexSpec, existedColumnNameSet,
                    "@EliasTable.indexes"));
        }
        List<IndexSpec> fieldIndexSpecs = createFieldIndexSpecs(fields);
        for (IndexSpec fieldIndexSpec : fieldIndexSpecs) {
            putIndexSpec(indexSpecMap, buildAndValidateIndexSpec(fieldIndexSpec, existedColumnNameSet,
                    "@Index/@UniqueIndex"));
        }
        List<IndexSpec> result = new ArrayList<>(indexSpecMap.values());
        analyzeRedundantIndexes(result);
        return result;
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
        Optional<SpecBuilderFactory> factory = factories.stream().
                filter(f -> f.fit(fieldTypeName, field))
                .findFirst();
        if (factory.isEmpty()) {
            logger.info("No suitable SpecBuilderFactory found for field {} with type {}, fallback to TEXT", field.getName(), fieldTypeName);
            return new TextSpecBuilderFactory().builder(field, true).build();
        }
        return factory.get().builder(field).build();
    }

    private static List<IndexSpec> createFieldIndexSpecs(Set<Field> fields) {
        List<FieldIndexDefinition> definitions = fields.stream()
                .flatMap(field -> Arrays.stream(field.getAnnotationsByType(Index.class))
                        .map(tableIndex -> new FieldIndexDefinition(
                                SpecUtils.getColumnName(field),
                                tableIndex.group(),
                                tableIndex.pos(),
                                tableIndex.desc() ? "DESC" : "ASC",
                                tableIndex.name(),
                                false
                        )))
                .toList();
        List<FieldIndexDefinition> uniqueDefinitions = fields.stream()
                .flatMap(field -> Arrays.stream(field.getAnnotationsByType(UniqueIndex.class))
                        .map(uniqueIndex -> new FieldIndexDefinition(
                                SpecUtils.getColumnName(field),
                                uniqueIndex.group(),
                                uniqueIndex.pos(),
                                uniqueIndex.desc() ? "DESC" : "ASC",
                                uniqueIndex.name(),
                                true
                        )))
                .toList();
        definitions = new ArrayList<>(definitions);
        definitions.addAll(uniqueDefinitions);
        List<IndexSpec> result = new ArrayList<>();
        definitions.stream()
                .filter(def -> StringUtils.isBlank(def.group()))
                .map(def -> new IndexSpec()
                        .setName(def.name())
                        .setUnique(def.unique())
                        .setColumns(def.columnName() + " " + def.order()))
                .forEach(result::add);

        Map<String, List<FieldIndexDefinition>> groupDefinitions = definitions.stream()
                .filter(def -> StringUtils.isNotBlank(def.group()))
                .collect(Collectors.groupingBy(FieldIndexDefinition::group));
        for (Map.Entry<String, List<FieldIndexDefinition>> entry : groupDefinitions.entrySet()) {
            result.add(buildGroupedIndex(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static IndexSpec buildGroupedIndex(String groupName,
                                               List<FieldIndexDefinition> definitions) {
        if (definitions.isEmpty()) {
            throw new IllegalStateException("Empty index group found: " + groupName);
        }
        validateGroupedDefinitions(groupName, definitions);
        Set<Integer> seenPositions = new HashSet<>();
        boolean hasDuplicatePosition = definitions.stream()
                .map(FieldIndexDefinition::position)
                .anyMatch(position -> !seenPositions.add(position));
        if (hasDuplicatePosition) {
            logger.warn("Duplicate position in index group {}, resolved by column name order.",
                    groupName);
        }
        List<FieldIndexDefinition> sorted = definitions.stream()
                .sorted(Comparator.comparingInt(FieldIndexDefinition::position)
                        .thenComparing(FieldIndexDefinition::columnName))
                .toList();
        String columns = sorted.stream()
                .map(def -> def.columnName() + " " + def.order())
                .collect(Collectors.joining(", "));
        boolean unique = sorted.stream().anyMatch(FieldIndexDefinition::unique);
        String declaredName = sorted.stream()
                .map(FieldIndexDefinition::name)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
        String generatedName = makeIndexName(unique,
                sorted.stream().map(FieldIndexDefinition::columnName).toList());
        return new IndexSpec()
                .setName(StringUtils.defaultIfBlank(declaredName, generatedName))
                .setUnique(unique)
                .setColumns(columns);
    }

    private static void validateGroupedDefinitions(String groupName,
                                                   List<FieldIndexDefinition> definitions) {
        Set<Boolean> uniqueSet = definitions.stream()
                .map(FieldIndexDefinition::unique)
                .collect(Collectors.toSet());
        if (uniqueSet.size() > 1) {
            throw new IllegalStateException(
                    "Conflicting unique flags in index group: " + groupName);
        }
        Set<String> names = definitions.stream()
                .map(FieldIndexDefinition::name)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (names.size() > 1) {
            throw new IllegalStateException(
                    "Conflicting index names in index group: " + groupName);
        }
    }

    private static IndexSpec buildAndValidateIndexSpec(IndexSpec sourceSpec,
                                                       Set<String> existedColumnNameSet,
                                                       String source) {
        String columnList = sourceSpec.getColumns();
        if (StringUtils.isBlank(columnList)) {
            throw new IllegalStateException("Index column list is empty in " + source);
        }
        List<String> annotatedColumns = parseColumnNames(columnList);
        if (annotatedColumns.stream().anyMatch(c -> !existedColumnNameSet.contains(c))) {
            throw new IllegalStateException("Annotated column not found in " + source + ": "
                    + String.join(", ", annotatedColumns));
        }
        String indexName = StringUtils.isBlank(sourceSpec.getName())
                ? makeIndexName(sourceSpec.isUnique(), annotatedColumns)
                : sourceSpec.getName();
        return new IndexSpec()
                .setName(limitIndexName(indexName))
                .setUnique(sourceSpec.isUnique())
                .setColumns(columnList);
    }

    private static List<String> parseColumnNames(String columnList) {
        return Arrays.stream(columnList.split(","))
                .map(columnSpec -> columnSpec.trim().split("\\s+")[0])
                .toList();
    }

    private static String makeIndexName(boolean unique, List<String> annotatedColumns) {
        return (unique ? "uk_" : "idx_") + String.join("_", annotatedColumns);
    }

    private static String limitIndexName(String indexName) {
        if (indexName.length() <= 64) {
            return indexName;
        }
        // 索引名称超过 64 个字符截断，可能会导致重名
        return indexName.substring(0, 64);
    }

    private static void putIndexSpec(Map<String, IndexSpec> indexSpecMap, IndexSpec indexSpec) {
        IndexSpec existed = indexSpecMap.get(indexSpec.getName());
        if (existed != null && !Objects.equals(existed, indexSpec)) {
            throw new IllegalStateException("Duplicate index name with different definitions: "
                    + indexSpec.getName());
        }
        indexSpecMap.put(indexSpec.getName(), indexSpec);
    }

    private static void analyzeRedundantIndexes(List<IndexSpec> indexSpecs) {
        List<NormalizedIndex> normalizedIndexes = indexSpecs.stream()
                .map(index -> new NormalizedIndex(
                        index.getName(),
                        index.isUnique(),
                        normalizeIndexColumns(index.getColumns())))
                .toList();
        for (int i = 0; i < normalizedIndexes.size(); i++) {
            for (int j = i + 1; j < normalizedIndexes.size(); j++) {
                NormalizedIndex left = normalizedIndexes.get(i);
                NormalizedIndex right = normalizedIndexes.get(j);
                if (left.unique() == right.unique()
                        && Objects.equals(left.columnsWithOrder(), right.columnsWithOrder())) {
                    logger.warn("Duplicate index definitions found: {} and {}", left.name(),
                            right.name());
                    continue;
                }
                if (left.unique() == right.unique()) {
                    if (isPrefix(left.columnsWithOrder(), right.columnsWithOrder())) {
                        logger.warn("Potential redundant prefix index: {} is covered by {}",
                                left.name(), right.name());
                    } else if (isPrefix(right.columnsWithOrder(), left.columnsWithOrder())) {
                        logger.warn("Potential redundant prefix index: {} is covered by {}",
                                right.name(), left.name());
                    }
                }
                if (Objects.equals(left.columnsWithOrder(), right.columnsWithOrder())
                        && left.unique() != right.unique()) {
                    logger.warn("Unique and non-unique indexes share same columns: {} and {}",
                            left.name(), right.name());
                }
                if (isReasonableParallelIndex(left.columnsWithOrder(), right.columnsWithOrder())) {
                    logger.info("Parallel index pattern detected, please verify necessity: {} and {}",
                            left.name(), right.name());
                }
            }
        }
    }

    private static boolean isReasonableParallelIndex(List<String> left, List<String> right) {
        if (left.size() == 1 && right.size() > 1) {
            return right.stream().skip(1).anyMatch(column -> column.equals(left.get(0)));
        }
        if (right.size() == 1 && left.size() > 1) {
            return left.stream().skip(1).anyMatch(column -> column.equals(right.get(0)));
        }
        return false;
    }

    private static boolean isPrefix(List<String> prefix, List<String> full) {
        if (prefix.size() >= full.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!Objects.equals(prefix.get(i), full.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> normalizeIndexColumns(String columns) {
        return Arrays.stream(columns.split(","))
                .map(item -> item.trim())
                .filter(item -> !item.isBlank())
                .map(item -> item.split("\\s+")[0])
                .toList();
    }

    private record FieldIndexDefinition(String columnName, String group, int position, String order,
                                        String name, boolean unique) {
    }

    private record NormalizedIndex(String name, boolean unique, List<String> columnsWithOrder) {
    }
}