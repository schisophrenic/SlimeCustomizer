package io.ncbpfluffybear.slimecustomizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Installs default configuration files on a clean server without overwriting
 * customized files or saved items on subsequent restarts.
 */
public final class ConfigFileInstaller {

    public static final List<String> DEFAULT_FILES = List.of(
            "categories.yml",
            "items.yml",
            "mob-drops.yml",
            "machines.yml",
            "generators.yml",
            "solar-generators.yml"
    );

    private ConfigFileInstaller() {}

    public static void install(Path dataFolder, ClassLoader resources) throws IOException {
        Objects.requireNonNull(dataFolder, "dataFolder");
        Objects.requireNonNull(resources, "resources");

        // The parent folder must exist before Files.copy or saveditems creation.
        Files.createDirectories(dataFolder);
        Files.createDirectories(dataFolder.resolve("saveditems"));

        for (String filename : DEFAULT_FILES) {
            Path destination = dataFolder.resolve(filename);

            if (Files.exists(destination)) {
                if (!Files.isRegularFile(destination)) {
                    throw new IOException("Expected a configuration file but found a directory: " + destination);
                }
                continue; // Existing administrator configuration always wins.
            }

            try (InputStream source = resources.getResourceAsStream(filename)) {
                if (source == null) {
                    throw new IOException("Missing bundled configuration resource: " + filename);
                }
                Files.copy(source, destination);
            }
        }
    }
}
