package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.io.IOException;

public interface SQLGenerator {

    String createTable(TableSpec tableSpec) throws IOException;

    String addColumn(String tableName, ColumnSpec columnSpec) throws IOException;

    String modifyColumn(String tableName, String columnName,
        ColumnModifySpec columnModifySpec) throws IOException;
}