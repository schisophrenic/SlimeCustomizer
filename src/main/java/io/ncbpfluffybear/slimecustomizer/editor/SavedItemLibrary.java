package io.ncbpfluffybear.slimecustomizer.editor;

import io.ncbpfluffybear.slimecustomizer.SlimeCustomizer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** One item template catalog for /sc item and the admin GUI. */
public final class SavedItemLibrary {
    private final File folder;

    public SavedItemLibrary(SlimeCustomizer plugin) {
        this.folder = new File(plugin.getDataFolder(), "saveditems");
    }

    public List<String> list() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();
        List<String> ids = new ArrayList<>();
        Arrays.stream(files).filter(File::isFile)
                .map(File::getName).map(n -> n.substring(0, n.length() - 4))
                .forEach(ids::add);
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public ItemStack get(String id) {
        if (!safeId(id)) return null;
        return YamlConfiguration.loadConfiguration(new File(folder, id + ".yml"))
                .getItemStack("item");
    }

    public String saveHand(Player player) throws IOException {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            throw new IllegalArgumentException("Hold a real item in your main hand first.");
        }
        Files.createDirectories(folder.toPath());
        int next = 0;
        while (new File(folder, next + ".yml").exists()) next++;
        String id = Integer.toString(next);
        File destination = new File(folder, id + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", hand.clone()); // Includes PDC, component data, amount, custom model.
        config.save(destination);
        return id;
    }

    private static boolean safeId(String id) {
        return id != null && !id.isBlank() && id.matches("[A-Za-z0-9_-]+");
    }
}
