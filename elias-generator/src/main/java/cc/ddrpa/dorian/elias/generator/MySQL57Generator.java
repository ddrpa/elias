package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.StringLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class MySQL57Generator implements SQLGenerator {

    private static final String CREATE_TABLE_TEMPLATE = """
            {% if dropIfExists %}drop table if exists `{{ t.name }}`;
            {% endif %}
            create table `{{ t.name }}` (
            {% for c in t.columns %}
              `{{ c.name }}` {{ c.columnType }}{% if c.nullable %} null{% else %} not null{% endif %}{% if c.defaultValue %} default '{{ c.defaultValue }}'{% endif %}{% if c.autoIncrement %} auto_increment{% endif %}
              {% if c.primaryKey %}  primary key{% endif %}{% if not loop.last %},{% endif %}
            
            {% endfor %}
            );
            {% for i in t.indexes %}
            create{% if i.unique %} unique{% endif %} index {{ i.name }} on `{{ t.name }}` ({{ i.columns }});
            {% endfor %}
            {% for s in t.spatialIndexSpecs %}
            create spatial index {{ s.name }} on `{{ t.name }}` ({{ s.columns }});
            {% endfor %}
            """;
    private static final String CREATE_H2_TABLE_TEMPLATE = """
            {% if dropIfExists %}drop table if exists {{ t.name }};
            {% endif %}
            create table {{ t.name }} (
            {% for c in t.columns %}
              {{ c.name }} {{ c.columnType }}{% if c.nullable %} null{% else %} not null{% endif %}{% if c.defaultValue %} default '{{ c.defaultValue }}'{% endif %}{% if c.autoIncrement %} auto_increment{% endif %}
              {% if c.primaryKey %}  primary key{% endif %}{% if not loop.last %},{% endif %}
            
            {% endfor %}
            );
            {% for i in t.indexes %}
            create{% if i.unique %} unique{% endif %} index {{ i.name }} on {{ t.name }} ({{ i.columns }});
            {% endfor %}
            {% for s in t.spatialIndexSpecs %}
            create spatial index {{ s.name }} on {{ t.name }} ({{ s.columns }});
            {% endfor %}
            """;
    private static final String ADD_COLUMN_TEMPLATE = """
            alter table `{{ table }}` add column `{{ c.name }}` {{ c.columnType }}{% if c.nullable %} null{% else %} not null{% endif %}{% if c.defaultValue %} default '{{ c.defaultValue }}'{% endif %}{% if c.autoIncrement %} auto_increment{% endif %}{% if c.primaryKey %}
                primary key{% endif %};
            """;
    private static final String MODIFY_COLUMN_TEMPLATE = """
            alter table `{{ table }}` modify column `{{ column }}`{% if cm.alterColumnType %} {{ cm.newColumnType }}{% endif %}{% if cm.alterNullable %}{% if cm.newNullable %} null{% else %} not null{% endif %}{% endif %}{% if cm.alterDefaultValue %}{% if cm.newDefaultValue %} default '{{ cm.newDefaultValue }}'{% else %} null{% endif %}{% endif %};
            """;

    private final PebbleTemplate createTableTemplate;
    private final PebbleTemplate createH2TableTemplate;
    private final PebbleTemplate addColumnTemplate;
    private final PebbleTemplate modifyColumnTemplate;
    private boolean dropIfExists = true;
    private boolean h2Compatibility = false;

    public MySQL57Generator() {
        PebbleEngine engine = new PebbleEngine.Builder()
                .loader(new StringLoader())
                .build();
        this.createTableTemplate = engine.getTemplate(CREATE_TABLE_TEMPLATE);
        this.createH2TableTemplate = engine.getTemplate(CREATE_H2_TABLE_TEMPLATE);
        this.addColumnTemplate = engine.getTemplate(ADD_COLUMN_TEMPLATE);
        this.modifyColumnTemplate = engine.getTemplate(MODIFY_COLUMN_TEMPLATE);
    }

    public MySQL57Generator setDropIfExists(boolean dropIfExists) {
        this.dropIfExists = dropIfExists;
        return this;
    }

    @Override
    public String createTable(TableSpec tableSpec) throws IOException {
        StringWriter writer = new StringWriter();
        Map<String, Object> context = new HashMap<>();
        context.put("dropIfExists", dropIfExists);
        if (h2Compatibility) {
            for (ColumnSpec col : tableSpec.getColumns()) {
                String columnType = col.getColumnType();
                if (columnType.contains("int")) {
                    col.setColumnType(col.getDataType());
                }
                // TODO 如果有 H2 关键字，抛出异常
            }
            context.put("t", tableSpec);
            createH2TableTemplate.evaluate(writer, context);
        } else {
            context.put("t", tableSpec);
            createTableTemplate.evaluate(writer, context);
        }
        return writer.toString();
    }

    @Override
    public String addColumn(String tableName, ColumnSpec columnSpec) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("table", tableName);
        context.put("c", columnSpec);
        StringWriter writer = new StringWriter();
        addColumnTemplate.evaluate(writer, context);
        return writer.toString();
    }

    @Override
    public String modifyColumn(String tableName, String columnName,
                               ColumnModifySpec columnModifySpec) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("table", tableName);
        context.put("column", columnName);
        context.put("cm", columnModifySpec);
        StringWriter writer = new StringWriter();
        modifyColumnTemplate.evaluate(writer, context);
        return writer.toString();
    }

    /**
     * 开启 H2 兼容模式，生成的 SQL 可以在 H2 数据库中执行
     *
     * @return
     */
    public MySQL57Generator enableH2Compatibility() {
        this.h2Compatibility = true;
        return this;
    }
}