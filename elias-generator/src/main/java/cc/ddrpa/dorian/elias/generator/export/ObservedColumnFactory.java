package cc.ddrpa.dorian.elias.generator.export;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.validation.ColumnProperties;

/**
 * Builds {@link ColumnSpec} / {@link ColumnModifySpec} from JDBC-observed column metadata
 * for rollback SQL generation.
 */
final class ObservedColumnFactory {

    private ObservedColumnFactory() {
    }

    static ColumnSpec toColumnSpec(ColumnProperties props) {
        ColumnSpec spec = new ColumnSpec()
                .setName(props.getName())
                .setDataType(props.getDataType())
                .setColumnType(props.getColumnType())
                .setNullable(Boolean.TRUE.equals(props.getNullable()));
        props.getDataLength().ifPresent(spec::setLength);
        props.getDefaultValueAsString().ifPresent(spec::setDefaultValue);
        return spec;
    }

    static ColumnModifySpec toModifySpec(ColumnProperties props) {
        return new ColumnModifySpec()
                .setColumnType(props.getColumnType())
                .setNullable(Boolean.TRUE.equals(props.getNullable()))
                .setDefaultValue(props.getDefaultValueAsString().orElse(null));
    }
}
