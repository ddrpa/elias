package cc.ddrpa.dorian.elias.configuration;

import cc.ddrpa.dorian.elias.EntityExtractor;
import cc.ddrpa.dorian.elias.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.spec.SpecMaker;
import cc.ddrpa.dorian.elias.spec.TableSpec;
import cc.ddrpa.dorian.elias.validation.ColumnCheckProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(EliasProperties.class)
public class EliasAutoConfiguration {

    private final EliasProperties properties;
    private final DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public EliasAutoConfiguration(EliasProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void databaseCheck() {
        // 扫描项目代码，确定 List<TableSpec>
        List<TableSpec> tableSpecList = properties.getPackages().stream()
            .map(EntityExtractor::findAll)
            .flatMap(Set::stream)
            .map(SpecMaker::makeTableSpec)
            .collect(Collectors.toList());
        // 确认数据库中存在这些表且 schema 正确
        tableSpecList.forEach(tableSpec -> {
            checkTable(tableSpec);
        });
    }

    private void checkTable(TableSpec tableSpec) {
        String sql = "select COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = ?";
        List<Map<String, Object>> rawColumnDetails = jdbcTemplate.queryForList(sql, tableSpec.getName());
        Map<String, ColumnCheckProperties> sqlColumnMap = rawColumnDetails.stream()
            .map(rawMap -> new ColumnCheckProperties(rawMap))
            .collect(Collectors.toMap(ColumnCheckProperties::getName, column -> column));
        // 实体类的所有有效属性都需要在数据库中存在
        assert sqlColumnMap.keySet().containsAll(tableSpec.getColumns().stream().map(ColumnSpec::getName).collect(Collectors.toSet()));
        // 遍历实体类的所有属性，检查类型等定义
        tableSpec.getColumns().forEach(columnSpec -> {
            ColumnCheckProperties columnCheckProperties = sqlColumnMap.get(columnSpec.getName());
            columnCheckProperties.validate(columnSpec);
        });
    }
}