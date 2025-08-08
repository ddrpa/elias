package cc.ddrpa.dorian.elias.core.factory;

import cc.ddrpa.dorian.elias.core.spec.ColumnSpecBuilder;
import java.lang.reflect.Field;
import java.util.List;

public class DateTimeSpecBuilderFactory implements SpecBuilderFactory {

    private static final List<String> ACCEPTED_DATE_TYPE = List.of(
        "java.time.LocalDate",
        "java.sql.Date"

    );
    private static final List<String> ACCEPTED_TIME_TYPE = List.of(
        "java.sql.Time",
        "java.time.LocalTime"
    );
    private static final List<String> ACCEPTED_DATETIME_TYPE = List.of(
        "java.time.LocalDateTime",
        "java.time.OffsetDateTime",
        "java.time.ZonedDateTime",
        "java.sql.Timestamp",
        "java.util.Date",
        "java.time.Instant"
    );

    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return ACCEPTED_DATE_TYPE.contains(fieldTypeName)
            || ACCEPTED_TIME_TYPE.contains(fieldTypeName)
            || ACCEPTED_DATETIME_TYPE.contains(fieldTypeName);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        String fieldType = field.getType().getName();
        ColumnSpecBuilder builder = SpecBuilderFactory.super.builder(field);
        if (ACCEPTED_DATE_TYPE.contains(fieldType)) {
            builder.setDataType("date");
        } else if (ACCEPTED_TIME_TYPE.contains(fieldType)) {
            builder.setDataType("time");
        } else if (ACCEPTED_DATETIME_TYPE.contains(fieldType)) {
            builder.setDataType("datetime");
        }
        return builder;
    }
}