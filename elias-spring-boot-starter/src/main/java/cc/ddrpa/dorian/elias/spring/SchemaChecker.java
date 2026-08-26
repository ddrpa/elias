package cc.ddrpa.dorian.elias.spring;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpecBuilder;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.validation.ColumnProperties;
import cc.ddrpa.dorian.elias.core.validation.IndexProperties;
import cc.ddrpa.dorian.elias.core.validation.SchemaDefinitionIssue;
import cc.ddrpa.dorian.elias.core.validation.SchemaDefinitionValidator;
import cc.ddrpa.dorian.elias.core.validation.mismatch.ISpecMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnNotExistMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.IllegalIdentifierMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.IndexNotExistMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.IndexSpecMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.InvalidIndexDefinitionMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ReservedKeywordMismatch;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.TableNotExistMismatch;
import cc.ddrpa.dorian.elias.generator.MySQL57Generator;
import cc.ddrpa.dorian.elias.generator.SQLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaChecker {

    private static final String FETCH_METADATA_SQL = "select COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, COLUMN_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = ? and TABLE_NAME = ?";
    private static final String FETCH_INDEX_METADATA_SQL = "select INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, COLLATION from INFORMATION_SCHEMA.STATISTICS where TABLE_SCHEMA = ? and TABLE_NAME = ? order by INDEX_NAME, SEQ_IN_INDEX";
    private static final Logger logger = LoggerFactory.getLogger(SchemaChecker.class);
    private static final SchemaDefinitionValidator DEFINITION_VALIDATOR = new SchemaDefinitionValidator();
    private final JdbcTemplate jdbcTemplate;
    private final String schema;
    private final List<TableSpec> tableSpecList = new ArrayList<>();
    private boolean autoFix = false;

    public SchemaChecker(JdbcTemplate jdbcTemplate) throws SQLException {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = jdbcTemplate.getDataSource().getConnection().getCatalog();
    }

    public SchemaChecker setAutoFix(boolean autoFix) {
        this.autoFix = autoFix;
        return this;
    }

    public SchemaChecker addTableSpecies(List<TableSpec> specList) {
        this.tableSpecList.addAll(specList);
        return this;
    }

    public boolean check() throws IOException {
        SQLGenerator generator = new MySQL57Generator().setDropIfExists(false);
        boolean somethingBadHappened = false;
        for (TableSpec tableSpec : tableSpecList) {
            List<ISpecMismatch> definitionMismatches = validateDefinition(tableSpec);
            if (!definitionMismatches.isEmpty()) {
                somethingBadHappened = true;
                definitionMismatches.forEach(mismatch -> logger.warn("{}", mismatch.errorMessage()));
                continue;
            }
            List<ISpecMismatch> mismatches = tableCheck(tableSpec);
            if (mismatches.isEmpty()) {
                continue;
            }
            somethingBadHappened = true;
            if (mismatches.get(0) instanceof TableNotExistMismatch mismatch) {
                // 表不存在，创建表
                String createTableSql = generator.createTable(mismatch.getExpectedTableSpec());
                errorAndRecommend(mismatch.errorMessage(), createTableSql);
                if (autoFix) {
                    autoFixCreateTable(mismatch.getExpectedTableSpec().getName(), createTableSql);
                }
                // 因为表不存在，显然就不会有什么列定义不匹配的问题了，跳过后续处理
                continue;
            }
            for (ISpecMismatch mismatch : mismatches) {
                if (mismatch instanceof ColumnNotExistMismatch columnNotExistMismatch) {
                    // 缺列，创建列
                    String addColumnSql = generator.addColumn(
                            columnNotExistMismatch.getTableName(),
                            columnNotExistMismatch.getColumnSpec());
                    errorAndRecommend(mismatch.errorMessage(), addColumnSql);
                    if (autoFix) {
                        autoFixAddColumn(
                                columnNotExistMismatch.getTableName(),
                                columnNotExistMismatch.getColumnSpec().getName(),
                                addColumnSql);
                    }
                } else if (mismatch instanceof ColumnSpecMismatch columnSpecMismatch) {
                    // 列的属性不匹配
                    ColumnModifySpec columnModifySpecResult = ColumnModifySpecBuilder.build(
                            columnSpecMismatch);
                    String modifyColumnSql = generator.modifyColumn(
                            columnSpecMismatch.getTableName(),
                            columnSpecMismatch.getColumnName(),
                            columnModifySpecResult);
                    // TODO is autofix enabled and autoFix is processable?
                    if (columnModifySpecResult.isAutoFixEnabled()) {
                        errorAndRecommend(mismatch.errorMessage(), modifyColumnSql);
                        if (autoFix) {
                            autoFixModifyColumn(
                                    columnSpecMismatch.getTableName(),
                                    columnSpecMismatch.getColumnName(),
                                    modifyColumnSql);
                        }
                    } else {
                        logger.warn(
                                "{}\nAuto-fix is not recommended due to:\n{}\nEnsure all values fit within the new constraints and try:\n{}",
                                mismatch.errorMessage(),
                                String.join("\n", columnModifySpecResult.getWarnings()),
                                modifyColumnSql);
                    }
                } else if (mismatch instanceof IndexNotExistMismatch indexNotExistMismatch) {
                    String createIndexSql = generator.createIndex(
                            indexNotExistMismatch.getTableName(),
                            indexNotExistMismatch.getIndexSpec());
                    errorAndRecommend(mismatch.errorMessage(), createIndexSql);
                    if (autoFix) {
                        autoFixCreateIndex(
                                indexNotExistMismatch.getTableName(),
                                indexNotExistMismatch.getIndexSpec().getName(),
                                createIndexSql);
                    }
                } else if (mismatch instanceof IndexSpecMismatch indexSpecMismatch) {
                    String updateIndexSql = generator.dropIndex(
                            indexSpecMismatch.getTableName(),
                            indexSpecMismatch.getIndexName()) + "\n" + generator.createIndex(
                            indexSpecMismatch.getTableName(),
                            indexSpecMismatch.getExpectedIndexSpec());
                    errorAndRecommend(mismatch.errorMessage(), updateIndexSql);
                    if (autoFix) {
                        autoFixUpdateIndex(
                                indexSpecMismatch.getTableName(),
                                indexSpecMismatch.getIndexName(),
                                updateIndexSql);
                    }
                }
            }
        }
        return somethingBadHappened;
    }

    private List<ISpecMismatch> tableCheck(TableSpec tableSpec) {
        // 对指定表，获取数据库中的元数据
        List<Map<String, Object>> rawColumnDetails = jdbcTemplate.queryForList(FETCH_METADATA_SQL,
                this.schema, tableSpec.getName());
        if (rawColumnDetails.isEmpty()) {
            // 数据库中不存在这个表
            return List.of(new TableNotExistMismatch(tableSpec));
        }
        List<ISpecMismatch> mismatches = new ArrayList<>();
        // 将表转换为 Map<ColumnName, ColumnProperties>
        Map<String, ColumnProperties> sqlColumnMap = rawColumnDetails.stream()
                .map(ColumnProperties::new)
                .collect(Collectors.toMap(ColumnProperties::getName, column -> column));
        // 遍历实体类的所有属性，检查类型等定义
        for (ColumnSpec columnSpec : tableSpec.getColumns()) {
            if (!sqlColumnMap.containsKey(columnSpec.getName())) {
                // 找不到定义的列
                mismatches.add(new ColumnNotExistMismatch(tableSpec.getName(), columnSpec));
                continue;
            }
            // 实体类的所有有效属性都需要在数据库中存在
            ColumnProperties columnProperties = sqlColumnMap.get(columnSpec.getName());
            Optional<ColumnSpecMismatch> mismatch = columnProperties.validate(columnSpec);
            mismatch.ifPresent(columnSpecMismatch -> mismatches.add(columnSpecMismatch
                    .setTableName(tableSpec.getName())
                    .setColumnName(columnSpec.getName())));
        }
        Map<String, IndexProperties> sqlIndexMap = fetchIndexProperties(tableSpec.getName());
        for (IndexSpec expectedIndex : tableSpec.getIndexes()) {
            IndexProperties actualIndex = sqlIndexMap.get(
                    expectedIndex.getName().toLowerCase(Locale.ROOT));
            if (actualIndex == null) {
                mismatches.add(new IndexNotExistMismatch(tableSpec.getName(), expectedIndex));
                continue;
            }
            actualIndex.validate(expectedIndex)
                    .ifPresent(indexSpecMismatch -> mismatches.add(indexSpecMismatch
                            .setTableName(tableSpec.getName())
                            .setIndexName(expectedIndex.getName())
                            .setExpectedIndexSpec(expectedIndex)));
        }
        return mismatches;
    }

    private void errorAndRecommend(String errorMessage, String recommendation) {
        logger.warn("{}\nRecommending fix with:\n{}", errorMessage, recommendation);
    }

    private List<ISpecMismatch> validateDefinition(TableSpec tableSpec) {
        return DEFINITION_VALIDATOR.validate(tableSpec).stream()
                .map(issue -> toMismatch(tableSpec.getName(), issue))
                .toList();
    }

    private ISpecMismatch toMismatch(String tableName, SchemaDefinitionIssue issue) {
        return switch (issue.getType()) {
            case RESERVED_KEYWORD ->
                    new ReservedKeywordMismatch(tableName, issue.getSubject(), issue.getDetail());
            case ILLEGAL_IDENTIFIER ->
                    new IllegalIdentifierMismatch(tableName, issue.getSubject(), issue.getDetail());
            case INVALID_INDEX_DEFINITION ->
                    new InvalidIndexDefinitionMismatch(tableName, issue.getSubject(), issue.getDetail());
        };
    }

    private void autoFixCreateTable(String tableName, String sql) {
        executeMultiSQL(sql);
        logger.warn("Applying auto-fix…… Table `{}` created.", tableName);
    }

    private void autoFixAddColumn(String tableName, String columnName, String sql) {
        executeMultiSQL(sql);
        logger.warn("Applying auto-fix…… Column `{}` added in table `{}`.", columnName, tableName);
    }

    private void autoFixModifyColumn(String tableName, String columnName, String sql) {
        executeMultiSQL(sql);
        logger.warn("Applying auto-fix…… Column `{}` modified in table `{}`.", columnName,
                tableName);
    }

    private void autoFixCreateIndex(String tableName, String indexName, String sql) {
        executeMultiSQL(sql);
        logger.warn("Applying auto-fix…… Index `{}` created in table `{}`.", indexName, tableName);
    }

    private void autoFixUpdateIndex(String tableName, String indexName, String sql) {
        executeMultiSQL(sql);
        logger.warn("Applying auto-fix…… Index `{}` recreated in table `{}`.", indexName,
                tableName);
    }

    private Map<String, IndexProperties> fetchIndexProperties(String tableName) {
        List<Map<String, Object>> rawIndexDetails = jdbcTemplate.queryForList(FETCH_INDEX_METADATA_SQL,
                this.schema, tableName);
        Map<String, List<Map<String, Object>>> groupedRows = rawIndexDetails.stream()
                .filter(row -> !"PRIMARY".equals(row.get("INDEX_NAME")))
                .collect(Collectors.groupingBy(row -> String.valueOf(row.get("INDEX_NAME")),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, IndexProperties> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedRows.entrySet()) {
            List<String> orderedColumns = entry.getValue().stream()
                    .map(row -> {
                        String columnName = String.valueOf(row.get("COLUMN_NAME"));
                        String orderRaw = Objects.toString(row.get("COLLATION"), "A");
                        String order = "D".equalsIgnoreCase(orderRaw) ? "DESC" : "ASC";
                        return columnName + " " + order;
                    })
                    .toList();
            boolean unique = toInt(entry.getValue().get(0).get("NON_UNIQUE")) == 0;
            result.put(entry.getKey().toLowerCase(Locale.ROOT),
                    new IndexProperties(entry.getKey(), unique, orderedColumns));
        }
        return result;
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private void executeMultiSQL(String sql) {
        Arrays.stream(sql.split(";")).filter(s -> !s.isBlank()).forEach(jdbcTemplate::execute);
    }
}