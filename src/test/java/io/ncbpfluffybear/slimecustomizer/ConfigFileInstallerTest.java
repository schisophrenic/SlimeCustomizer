package io.ncbpfluffybear.slimecustomizer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileInstallerTest {

    @TempDir
    Path temp;

    @Test
    void cleanInstallCreatesFoldersAndAllDefaultFiles() throws IOException {
        Path data = temp.resolve("plugins").resolve("SlimeCustomizer");
        assertFalse(Files.exists(data));

        ConfigFileInstaller.install(data, ConfigFileInstallerTest.class.getClassLoader());

        assertTrue(Files.isDirectory(data.resolve("saveditems")));
        for (String name : ConfigFileInstaller.DEFAULT_FILES) {
            assertTrue(Files.isRegularFile(data.resolve(name)), name);
            assertTrue(Files.size(data.resolve(name)) > 0, name);
        }
    }

    @Test
    void existingConfigsAndSavedItemsAreNotOverwritten() throws IOException {
        Path data = temp.resolve("SlimeCustomizer");
        ConfigFileInstaller.install(data, ConfigFileInstallerTest.class.getClassLoader());

        Path customCategories = data.resolve("categories.yml");
        Path savedItem = data.resolve("saveditems").resolve("0.yml");
        Files.writeString(customCategories, "my_custom_category: true\n");
        Files.writeString(savedItem, "my_saved_item: true\n");
        Files.delete(data.resolve("machines.yml"));

        ConfigFileInstaller.install(data, ConfigFileInstallerTest.class.getClassLoader());

        assertEquals("my_custom_category: true\n", Files.readString(customCategories));
        assertEquals("my_saved_item: true\n", Files.readString(savedItem));
        assertTrue(Files.isRegularFile(data.resolve("machines.yml")),
                "A deleted default is regenerated without overwriting existing data");
    }

    @Test
    void missingBundledResourcesFailInsteadOfContinuingWithEmptyConfigs() {
        ClassLoader empty = new ClassLoader(null) {};
        Path data = temp.resolve("SlimeCustomizer");

        IOException thrown = assertThrows(IOException.class,
                () -> ConfigFileInstaller.install(data, empty));
        assertTrue(thrown.getMessage().contains("Missing bundled configuration resource"));
    }

    @Test
    void existingDirectoryAtConfigPathIsRejected() throws IOException {
        Path data = temp.resolve("SlimeCustomizer");
        Files.createDirectories(data.resolve("categories.yml"));

        IOException thrown = assertThrows(IOException.class,
                () -> ConfigFileInstaller.install(data, ConfigFileInstallerTest.class.getClassLoader()));
        assertTrue(thrown.getMessage().contains("Expected a configuration file"));
    }
}
