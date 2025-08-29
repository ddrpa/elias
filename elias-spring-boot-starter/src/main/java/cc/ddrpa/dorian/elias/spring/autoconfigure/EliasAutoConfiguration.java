package cc.ddrpa.dorian.elias.spring.autoconfigure;

import cc.ddrpa.dorian.elias.core.EntitySearcher;
import cc.ddrpa.dorian.elias.core.SpecMaker;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import cc.ddrpa.dorian.elias.spring.SchemaChecker;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * Spring Boot auto-configuration for Elias database schema validation.
 * 
 * <p>Automatically validates database schemas against entity classes at application startup
 * when {@code elias.validate.enable} is true. Can optionally auto-fix schema mismatches
 * based on configuration.
 * 
 * <p>This configuration activates when a {@link DataSource} is available and performs
 * entity discovery, specification generation, and database validation during the
 * {@link InitializingBean#afterPropertiesSet()} lifecycle phase.
 * 
 * @see EliasProperties
 * @see SchemaChecker
 */
@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${elias.validate.enable}")
@EnableConfigurationProperties(EliasProperties.class)
public class EliasAutoConfiguration implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(EliasAutoConfiguration.class);
    private static final String ASCII_ART = """
        
         _____ _ _
        |  ___| (_)
        | |__ | |_  __ _ ___
        |  __|| | |/ _` / __|
        | |___| | | (_| \\__ \\
        \\____/|_|_|\\__,_|___/
                      2.5.0-SNAPSHOT
        """;

    private final EliasProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public EliasAutoConfiguration(EliasProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        logger.info(ASCII_ART);
    }

    protected void schemaCheck() throws SQLException, IOException {
        List<String> includePackages = properties.getScan().getIncludes();
        if (includePackages.isEmpty()) {
            logger.warn("No package to scan, skip schema validation.");
            return;
        }
        EntitySearcher searcher = new EntitySearcher()
            .addPackages(includePackages);
        if (properties.getScan().getAcceptMybatisPlusTableNameAnnotation()) {
            searcher.useAnnotation(TableName.class);
        }
        List<TableSpec> tableSpecList = searcher
            .search()
            .stream()
            .map(SpecMaker::makeTableSpec)
            .collect(Collectors.toList());
        SchemaChecker checker = new SchemaChecker(jdbcTemplate)
            .addTableSpecies(tableSpecList)
            .setAutoFix(properties.isAutoFix());
        boolean somethingBadHappened = checker.check();
        if (properties.isStopOnMismatch() && somethingBadHappened) {
            logger.error(
                "Schema validation failed, starting terminated due to configuration. See logs for details.\n\n");
            throw new IllegalStateException(
                "Schema validation failed, starting terminated due to configuration. See logs for details.");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        schemaCheck();
    }
}