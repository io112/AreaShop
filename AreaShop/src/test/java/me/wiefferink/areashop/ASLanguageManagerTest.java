package me.wiefferink.areashop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ASLanguageManagerTest {

    private static final String FILE = "RU.yml";

    @TempDir
    File folder;

    private void update(String bundled) throws Exception {
        ASLanguageManager.updateLanguageFile(folder, FILE, bundled.getBytes(StandardCharsets.UTF_8));
    }

    private void writeDisk(String content) throws Exception {
        Files.writeString(new File(folder, FILE).toPath(), content);
    }

    private YamlConfiguration disk() {
        return YamlConfiguration.loadConfiguration(new File(folder, FILE));
    }

    @Test
    void savesMissingFile() throws Exception {
        update("a: one\n");

        assertEquals("one", disk().getString("a"));
        assertTrue(new File(folder, ASLanguageManager.BUNDLED_FOLDER + File.separator + FILE).isFile());
    }

    @Test
    void keepsEditsWhenBundledFileIsUnchanged() throws Exception {
        update("a: one\n");
        writeDisk("a: edited\n");

        update("a: one\n");

        assertEquals("edited", disk().getString("a"));
    }

    @Test
    void mergesPluginUpdateWithEdits() throws Exception {
        update("a: one\nb: two\nlist:\n- x\n");
        writeDisk("a: edited\nb: two\nlist:\n- x\n");

        update("a: one v2\nb: two v2\nc: three\nlist:\n- x\n- y\n");

        YamlConfiguration result = disk();
        assertEquals("edited", result.getString("a"));
        assertEquals("two v2", result.getString("b"));
        assertEquals("three", result.getString("c"));
        assertEquals(java.util.List.of("x", "y"), result.getStringList("list"));

        // The next start without changes keeps everything
        update("a: one v2\nb: two v2\nc: three\nlist:\n- x\n- y\n");
        assertEquals("edited", disk().getString("a"));
    }

    @Test
    void backsUpFilesFromOlderVersions() throws Exception {
        writeDisk("a: edited\n");

        update("a: one\n");

        assertEquals("one", disk().getString("a"));
        File[] backups = new File(folder, ASLanguageManager.BACKUP_FOLDER).listFiles();
        assertEquals(1, backups == null ? 0 : backups.length);
        assertEquals("a: edited\n", Files.readString(backups[0].toPath()));

        // Edits made after that are kept
        writeDisk("a: edited again\n");
        update("a: one\n");
        assertEquals("edited again", disk().getString("a"));
    }
}
