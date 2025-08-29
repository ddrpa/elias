package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.io.IOException;

/**
 * Interface for generating database-specific SQL DDL statements.
 * 
 * <p>Implementations provide database-specific SQL generation from Elias table specifications.
 * This allows support for different database systems with their specific syntax requirements.
 * 
 * @see cc.ddrpa.dorian.elias.generator.MySQL57Generator
 */
public interface SQLGenerator {

    /**
     * Generates CREATE TABLE statement for the given table specification.
     * 
     * @param tableSpec complete table specification including columns and indexes
     * @return SQL CREATE TABLE statement
     * @throws IOException if template processing fails
     */
    String createTable(TableSpec tableSpec) throws IOException;

    /**
     * Generates ALTER TABLE ADD COLUMN statement.
     * 
     * @param tableName target table name
     * @param columnSpec specification for the new column
     * @return SQL ALTER TABLE statement to add the column
     * @throws IOException if template processing fails
     */
    String addColumn(String tableName, ColumnSpec columnSpec) throws IOException;

    /**
     * Generates ALTER TABLE MODIFY COLUMN statement.
     * 
     * @param tableName target table name  
     * @param columnName name of column to modify
     * @param columnModifySpec modification specification
     * @return SQL ALTER TABLE statement to modify the column
     * @throws IOException if template processing fails
     */
    String modifyColumn(String tableName, String columnName,
        ColumnModifySpec columnModifySpec) throws IOException;
}