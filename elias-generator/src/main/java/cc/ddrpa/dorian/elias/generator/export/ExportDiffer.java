package cc.ddrpa.dorian.elias.generator.export;

import cc.ddrpa.dorian.elias.core.spec.ColumnComments;
import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpecBuilder;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.IndexSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.core.validation.ColumnProperties;
import cc.ddrpa.dorian.elias.core.validation.IndexProperties;
import cc.ddrpa.dorian.elias.core.validation.mismatch.impl.ColumnSpecMismatch;
import cc.ddrpa.dorian.elias.generator.MySQL57Generator;
import cc.ddrpa.dorian.elias.generator.SQLGenerator;
import cc.ddrpa.dorian.elias.generator.export.ExportChange.Kind;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Diffs expected {@link TableSpec}s (from {@code @EliasTable}) against a Liquibase-updated database.
 * Only tables named by entities are inspected; JDBC metadata is limited to the connection's
 * current catalog/schema so other databases are not scanned.
 * Emits destructive column changes (DROP / shortening) into the result; does not execute DDL.
 * <p>
 * Metadata is read via JDBC {@link DatabaseMetaData} so H2 ({@code MODE=MySQL}) and MySQL both work.
 * Type names are normalized to Elias/MySQL vocabulary to avoid false MODIFY noise on H2.
 */
public class ExportDiffer {

    private final Connection connection;
    private final SQLGenerator generator;
    /** Current JDBC catalog (MySQL database); may be null on some drivers. */
    private final String catalog;
    /** Current JDBC schema (e.g. H2 {@code PUBLIC}); often null on MySQL. */
    private final String schema;
    private final Map<String, String> renames = new LinkedHashMap<>();

    public ExportDiffer(Connection connection) throws SQLException {
        this(connection, new MySQL57Generator().setDropIfExists(false));
    }

    public ExportDiffer(Connection connection, SQLGenerator generator) throws SQLException {
        this.connection = connection;
        this.generator = generator;
        this.catalog = connection.getCatalog();
        this.schema = connection.getSchema();
    }

    /**
     * Declares column renames as {@code old:new} or {@code table.old:new}.
     */
    public ExportDiffer rename(String mapping) {
        if (mapping == null || mapping.isBlank()) {
            return this;
        }
        String[] parts = mapping.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "rename must be old:new or table.old:new, got: " + mapping);
        }
        renames.put(parts[0].trim(), parts[1].trim());
        return this;
    }

    public DiffResult diff(List<TableSpec> expectedTables) throws SQLException, IOException {
        DiffResult result = new DiffResult();
        for (TableSpec tableSpec : expectedTables) {
            diffTable(tableSpec, result);
        }
        return result;
    }

    private void diffTable(TableSpec tableSpec, DiffResult result)
            throws SQLException, IOException {
        List<Map<String, Object>> rawColumns = fetchColumns(tableSpec.getName());
        if (rawColumns.isEmpty()) {
            result.add(new ExportChange(
                    Kind.CREATE_TABLE,
                    "create table `" + tableSpec.getName() + "`",
                    generator.createTable(tableSpec),
                    generator.dropTable(tableSpec.getName()),
                    false));
            return;
        }

        Map<String, ColumnProperties> actualColumns = new LinkedHashMap<>();
        for (Map<String, Object> raw : rawColumns) {
            ColumnProperties props = new ColumnProperties(raw);
            actualColumns.put(props.getName().toLowerCase(Locale.ROOT), props);
        }

        Set<String> expectedColumnNames = tableSpec.getColumns().stream()
                .map(c -> c.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> handledActual = new HashSet<>();
        Set<String> handledExpected = new HashSet<>();
        for (Map.Entry<String, String> rename : renames.entrySet()) {
            String fromKey = rename.getKey();
            String to = rename.getValue().toLowerCase(Locale.ROOT);
            String from = (fromKey.contains(".")
                    ? fromKey.substring(fromKey.indexOf('.') + 1)
                    : fromKey).toLowerCase(Locale.ROOT);
            if (fromKey.contains(".")
                    && !fromKey.substring(0, fromKey.indexOf('.'))
                    .equalsIgnoreCase(tableSpec.getName())) {
                continue;
            }
            if (actualColumns.containsKey(from) && expectedColumnNames.contains(to)) {
                ColumnProperties fromProps = actualColumns.get(from);
                ColumnSpec expectedCol = tableSpec.getColumns().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(to))
                        .findFirst()
                        .orElseThrow();
                // CHANGE COLUMN is MySQL-only; exported for prod MySQL, not for H2 replay.
                String sql = String.format(
                        "alter table `%s` change column `%s` `%s` %s%s%s%s;",
                        tableSpec.getName(),
                        fromProps.getName(),
                        expectedCol.getName(),
                        expectedCol.getColumnType(),
                        expectedCol.isNullable() ? " null" : " not null",
                        expectedCol.getDefaultValue() != null
                                ? " default '" + expectedCol.getDefaultValue() + "'"
                                : "",
                        expectedCol.getSqlComment() != null
                                ? " comment '" + expectedCol.getSqlComment() + "'"
                                : "");
                String rollbackSql = String.format(
                        "alter table `%s` change column `%s` `%s` %s%s%s%s;",
                        tableSpec.getName(),
                        expectedCol.getName(),
                        fromProps.getName(),
                        fromProps.getColumnType(),
                        Boolean.TRUE.equals(fromProps.getNullable()) ? " null" : " not null",
                        fromProps.getDefaultValueAsString().isPresent()
                                ? " default '" + fromProps.getDefaultValueAsString().get() + "'"
                                : "",
                        fromProps.getComment()
                                .map(ColumnComments::forSqlLiteral)
                                .map(c -> " comment '" + c + "'")
                                .orElse(""));
                result.add(new ExportChange(
                        Kind.MODIFY_COLUMN,
                        "rename column `" + from + "` -> `" + to + "` on `" + tableSpec.getName()
                                + "`",
                        sql,
                        rollbackSql,
                        true));
                handledActual.add(from);
                handledExpected.add(to);
            }
        }

        for (ColumnSpec columnSpec : tableSpec.getColumns()) {
            String expectedName = columnSpec.getName().toLowerCase(Locale.ROOT);
            if (handledExpected.contains(expectedName)) {
                continue;
            }
            ColumnProperties actual = actualColumns.get(expectedName);
            if (actual == null) {
                result.add(new ExportChange(
                        Kind.ADD_COLUMN,
                        "add column `" + columnSpec.getName() + "` to `" + tableSpec.getName()
                                + "`",
                        generator.addColumn(tableSpec.getName(), columnSpec),
                        generator.dropColumn(tableSpec.getName(), columnSpec.getName()),
                        false));
                continue;
            }
            Optional<ColumnSpecMismatch> mismatch = actual.validate(columnSpec);
            if (mismatch.isPresent()) {
                ColumnSpecMismatch columnSpecMismatch = mismatch.get()
                        .setTableName(tableSpec.getName())
                        .setColumnName(columnSpec.getName());
                ColumnModifySpec modifySpec = ColumnModifySpecBuilder.build(columnSpecMismatch);
                boolean destructive = !modifySpec.isAutoFixEnabled();
                ColumnModifySpec rollbackModify = ObservedColumnFactory.toModifySpec(actual);
                result.add(new ExportChange(
                        Kind.MODIFY_COLUMN,
                        "modify column `" + columnSpec.getName() + "` on `" + tableSpec.getName()
                                + "`",
                        generator.modifyColumn(tableSpec.getName(), columnSpec.getName(),
                                modifySpec),
                        generator.modifyColumn(tableSpec.getName(), columnSpec.getName(),
                                rollbackModify),
                        destructive));
            }
        }

        for (Map.Entry<String, ColumnProperties> entry : actualColumns.entrySet()) {
            if (handledActual.contains(entry.getKey())) {
                continue;
            }
            if (!expectedColumnNames.contains(entry.getKey())) {
                ColumnProperties obsolete = entry.getValue();
                result.add(new ExportChange(
                        Kind.DROP_COLUMN,
                        "drop column `" + obsolete.getName() + "` from `"
                                + tableSpec.getName() + "`",
                        generator.dropColumn(tableSpec.getName(), obsolete.getName()),
                        generator.addColumn(tableSpec.getName(),
                                ObservedColumnFactory.toColumnSpec(obsolete)),
                        true));
            }
        }

        diffIndexes(tableSpec, result);
    }

    private void diffIndexes(TableSpec tableSpec, DiffResult result)
            throws SQLException, IOException {
        Map<String, IndexProperties> actualIndexes = fetchIndexProperties(tableSpec.getName());
        Set<String> expectedIndexNames = tableSpec.getIndexes().stream()
                .map(index -> index.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        for (IndexSpec indexSpec : tableSpec.getIndexes()) {
            IndexProperties actual = actualIndexes.get(indexSpec.getName().toLowerCase(Locale.ROOT));
            if (actual == null) {
                result.add(new ExportChange(
                        Kind.CREATE_INDEX,
                        "create index `" + indexSpec.getName() + "` on `" + tableSpec.getName()
                                + "`",
                        generator.createIndex(tableSpec.getName(), indexSpec),
                        generator.dropIndex(tableSpec.getName(), indexSpec.getName()),
                        false));
                continue;
            }
            if (actual.validate(indexSpec).isEmpty()) {
                continue;
            }
            result.add(new ExportChange(
                    Kind.DROP_INDEX,
                    "drop index `" + actual.getName() + "` on `" + tableSpec.getName()
                            + "` before recreate",
                    generator.dropIndex(tableSpec.getName(), actual.getName()),
                    generator.createIndex(tableSpec.getName(), actual.toIndexSpec()),
                    true));
            result.add(new ExportChange(
                    Kind.CREATE_INDEX,
                    "create index `" + indexSpec.getName() + "` on `" + tableSpec.getName()
                            + "`",
                    generator.createIndex(tableSpec.getName(), indexSpec),
                    generator.dropIndex(tableSpec.getName(), indexSpec.getName()),
                    false));
        }

        for (Map.Entry<String, IndexProperties> entry : actualIndexes.entrySet()) {
            if (expectedIndexNames.contains(entry.getKey())) {
                continue;
            }
            IndexProperties extra = entry.getValue();
            result.add(new ExportChange(
                    Kind.DROP_INDEX,
                    "drop extra index `" + extra.getName() + "` from `" + tableSpec.getName()
                            + "`",
                    generator.dropIndex(tableSpec.getName(), extra.getName()),
                    generator.createIndex(tableSpec.getName(), extra.toIndexSpec()),
                    true));
        }
    }

    private List<Map<String, Object>> fetchColumns(String tableName) throws SQLException {
        List<Map<String, Object>> rows = fetchColumnsWithPattern(tableName);
        if (rows.isEmpty()) {
            rows = fetchColumnsWithPattern(tableName.toLowerCase(Locale.ROOT));
        }
        if (rows.isEmpty()) {
            rows = fetchColumnsWithPattern(tableName.toUpperCase(Locale.ROOT));
        }
        for (Map<String, Object> row : rows) {
            JdbcTypeNormalizer.normalizeRow(row);
        }
        return rows;
    }

    private List<Map<String, Object>> fetchColumnsWithPattern(String tableNamePattern)
            throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(catalog, schema, tableNamePattern, null)) {
            while (rs.next()) {
                if (!inCurrentCatalogSchema(rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"))) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                row.put("COLUMN_DEFAULT", rs.getString("COLUMN_DEF"));
                row.put("IS_NULLABLE", rs.getString("IS_NULLABLE"));
                row.put("DATA_TYPE", rs.getString("TYPE_NAME"));
                int size = rs.getInt("COLUMN_SIZE");
                boolean sizeNull = rs.wasNull();
                row.put("CHARACTER_MAXIMUM_LENGTH", sizeNull || size <= 0 ? null : size);
                row.put("COLUMN_TYPE", null);
                row.put("COLUMN_COMMENT", rs.getString("REMARKS"));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Restricts metadata rows to the connection's catalog/schema. When the connection value is
     * null, that dimension is not filtered (driver-dependent); when both sides are non-null they
     * must match case-insensitively.
     */
    private boolean inCurrentCatalogSchema(String tableCatalog, String tableSchema) {
        if (catalog != null && tableCatalog != null
                && !catalog.equalsIgnoreCase(tableCatalog)) {
            return false;
        }
        if (schema != null && tableSchema != null
                && !schema.equalsIgnoreCase(tableSchema)) {
            return false;
        }
        return true;
    }

    private Map<String, IndexProperties> fetchIndexProperties(String tableName)
            throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        Map<String, IndexProperties> indexes = fetchIndexPropertiesForTable(meta, tableName);
        if (indexes.isEmpty()) {
            indexes = fetchIndexPropertiesForTable(meta, tableName.toLowerCase(Locale.ROOT));
        }
        if (indexes.isEmpty()) {
            indexes = fetchIndexPropertiesForTable(meta, tableName.toUpperCase(Locale.ROOT));
        }
        return indexes;
    }

    private Map<String, IndexProperties> fetchIndexPropertiesForTable(DatabaseMetaData meta,
                                                                      String tableName)
            throws SQLException {
        Map<String, List<ObservedIndexColumn>> grouped = new LinkedHashMap<>();
        try (ResultSet rs = meta.getIndexInfo(catalog, schema, tableName, false, false)) {
            while (rs.next()) {
                if (!inCurrentCatalogSchema(rs.getString("TABLE_CAT"),
                        rs.getString("TABLE_SCHEM"))) {
                    continue;
                }
                if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }
                String name = rs.getString("INDEX_NAME");
                if (name == null || isPrimaryIndex(name)) {
                    continue;
                }
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName == null || columnName.isBlank()) {
                    continue;
                }
                String ascOrDesc = rs.getString("ASC_OR_DESC");
                String order = "D".equalsIgnoreCase(ascOrDesc) ? "DESC" : "ASC";
                boolean unique = !rs.getBoolean("NON_UNIQUE");
                int ordinal = rs.getInt("ORDINAL_POSITION");
                grouped.computeIfAbsent(name, ignored -> new ArrayList<>())
                        .add(new ObservedIndexColumn(ordinal, columnName, order, unique));
            }
        }
        Map<String, IndexProperties> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<ObservedIndexColumn>> entry : grouped.entrySet()) {
            List<ObservedIndexColumn> rows = new ArrayList<>(entry.getValue());
            rows.sort(Comparator.comparingInt(ObservedIndexColumn::ordinal));
            List<String> orderedColumns = rows.stream()
                    .map(column -> column.columnName() + " " + column.order())
                    .toList();
            result.put(entry.getKey().toLowerCase(Locale.ROOT),
                    new IndexProperties(entry.getKey(), rows.get(0).unique(), orderedColumns));
        }
        return result;
    }

    private static boolean isPrimaryIndex(String indexName) {
        String upper = indexName.toUpperCase(Locale.ROOT);
        return "PRIMARY".equals(upper) || upper.startsWith("PRIMARY_KEY");
    }

    private record ObservedIndexColumn(int ordinal, String columnName, String order,
                                       boolean unique) {
    }
}
