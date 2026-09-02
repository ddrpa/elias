package cc.ddrpa.dorian.elias.generator.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquibaseSqlExporterTest {

    @TempDir
    Path temp;

    @Test
    void exportReturnsFalseAndWritesNothingWhenNoChanges() throws Exception {
        Path out = temp.resolve("changes/0001-empty.sql");
        LiquibaseSqlExporter exporter = new LiquibaseSqlExporter();
        assertFalse(exporter.export(new DiffResult(), out, "0001", "elias"));
        assertFalse(Files.exists(out));
    }

    @Test
    void exportReturnsFalseWhenChangesHaveBlankSql() throws Exception {
        DiffResult diff = new DiffResult();
        diff.add(new ExportChange(ExportChange.Kind.ADD_COLUMN, "noop", "  ", false));
        Path out = temp.resolve("changes/0002-blank.sql");
        assertFalse(new LiquibaseSqlExporter().export(diff, out, "0002", "elias"));
        assertFalse(Files.exists(out));
        assertFalse(diff.hasExportableSql());
    }

    @Test
    void exportWritesFileWhenSqlPresent() throws Exception {
        DiffResult diff = new DiffResult();
        diff.add(new ExportChange(
                ExportChange.Kind.ADD_COLUMN,
                "add c",
                "ALTER TABLE t ADD COLUMN c INT NULL",
                false));
        Path out = temp.resolve("changes/0003-ok.sql");
        assertTrue(new LiquibaseSqlExporter().export(diff, out, "0003", "elias"));
        assertTrue(Files.exists(out));
        String content = Files.readString(out);
        assertTrue(content.contains("ALTER TABLE t ADD COLUMN c INT NULL"));
    }
}
