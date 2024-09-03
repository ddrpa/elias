package cc.ddrpa.dorian.elias.generator;

import cc.ddrpa.dorian.elias.core.spec.ColumnModifySpec;
import cc.ddrpa.dorian.elias.core.spec.ColumnSpec;
import cc.ddrpa.dorian.elias.core.spec.TableSpec;
import java.io.StringWriter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class SQLGenerator {

    private static final Template createTableTemplate;
    private static final Template addColumnTemplate;
    private static final Template alterColumnTemplate;

    static {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class",
            ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        createTableTemplate = velocityEngine.getTemplate("create_table.vm");
        addColumnTemplate = velocityEngine.getTemplate("add_column.vm");
        alterColumnTemplate = velocityEngine.getTemplate("alter_column.vm");
    }

    public static String createTable(TableSpec tableSpec, boolean dropIfExists) {
        VelocityContext context = new VelocityContext();
        context.put("t", tableSpec);
        context.put("dropIfExists", dropIfExists);
        StringWriter writer = new StringWriter();
        createTableTemplate.merge(context, writer);
        return writer.toString();
    }

    public static String addColumn(String tableName, ColumnSpec columnSpec) {
        VelocityContext context = new VelocityContext();
        context.put("table", tableName);
        context.put("c", columnSpec);
        StringWriter writer = new StringWriter();
        addColumnTemplate.merge(context, writer);
        return writer.toString();
    }

    public static String modifyColumn(String tableName, String columnName,
        ColumnModifySpec columnModifySpec) {
        VelocityContext context = new VelocityContext();
        context.put("table", tableName);
        context.put("column", columnName);
        context.put("cm", columnModifySpec);
        StringWriter writer = new StringWriter();
        alterColumnTemplate.merge(context, writer);
        return writer.toString();
    }

}