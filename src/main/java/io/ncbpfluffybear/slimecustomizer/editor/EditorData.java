package io.ncbpfluffybear.slimecustomizer.editor;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.ncbpfluffybear.slimecustomizer.Registry;
import io.ncbpfluffybear.slimecustomizer.SlimeCustomizer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Editor persistence is intentionally separate from runtime recipe matching.
 * All recipe edits are staged in YAML and activated by a clean server restart.
 * Live sorting is safe because the maintained Slimefun-Legacy API exposes the
 * category tier and the item-group list.
 */
public final class EditorData {
    private static final String[] ITEM_FILES = {
        "items.yml", "machines.yml", "generators.yml",
        "solar-generators.yml", "mob-drops.yml"
    };
    private final SlimeCustomizer plugin;
    private final File orderFile;

    public EditorData(SlimeCustomizer plugin) {
        this.plugin = plugin;
        this.orderFile = new File(plugin.getDataFolder(), "editor-order.yml");
    }

    public List<String> categories() {
        YamlConfiguration order = YamlConfiguration.loadConfiguration(orderFile);
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "categories.yml"));
        List<String> configured = order.getStringList("category-order");
        java.util.LinkedHashSet<String> sorted = new java.util.LinkedHashSet<>();
        sorted.addAll(configured);
        // Show unregistered YAML categories too so admins can repair startup.
        sorted.addAll(defaults.getKeys(false));
        List<String> missing = new ArrayList<>(Registry.allItemGroups.keySet());
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(missing);
        return new ArrayList<>(sorted);
    }

    public List<String> items(String categoryId) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();

        // YAML is authoritative even when an earlier item fails registration.
        for (String filename : ITEM_FILES) {
            File file = new File(plugin.getDataFolder(), filename);
            YamlConfiguration source = YamlConfiguration.loadConfiguration(file);
            for (String id : source.getKeys(false)) {
                if (categoryId.equals(source.getString(id + ".category"))) {
                    result.add(id);
                }
            }
        }

        ItemGroup category = Registry.allItemGroups.get(categoryId);
        if (category != null) {
            for (SlimefunItem item : category.getItems()) {
                if (item.getAddon() != null && item.getAddon().getName().equals(plugin.getName())) {
                    result.add(item.getId());
                }
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        List<String> configured = config.getStringList("item-order." + categoryId);
        List<String> list = new ArrayList<>(result);
        list.sort(Comparator.comparingInt(k -> index(configured, k)));
        return list;
    }

    private static int index(List<String> order, String id) {
        int index = order.indexOf(id);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public boolean moveCategory(String id, int direction) throws IOException {
        List<String> current = categories();
        int from = current.indexOf(id);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= current.size()) return false;
        java.util.Collections.swap(current, from, to);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        config.set("category-order", current);
        config.save(orderFile);
        applyOrder();
        return true;
    }

    public boolean moveItem(String category, String id, int direction) throws IOException {
        List<String> current = items(category);
        int from = current.indexOf(id);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= current.size()) return false;
        java.util.Collections.swap(current, from, to);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        config.set("item-order." + category, current);
        config.save(orderFile);
        applyOrder();
        return true;
    }

    /** Only the categories owned by this addon are modified. */
    public void applyOrder() {
        List<String> groups = categories();
        boolean hasCustomOrder = YamlConfiguration.loadConfiguration(orderFile).contains("category-order");
        // A dedicated tier range ensures that the order remains stable across
        // Slimefun's category sort, regardless of addon key/name ordering.
        for (int i = 0; i < groups.size(); i++) {
            ItemGroup group = Registry.allItemGroups.get(groups.get(i));
            if (hasCustomOrder && group != null) group.setTier(100 + i);
        }
        for (String id : groups) {
            ItemGroup group = Registry.allItemGroups.get(id);
            if (group == null) continue;
            List<String> customOrder = items(id);
            group.getItems().sort(Comparator.comparingInt(
                    item -> index(customOrder, item.getId())));
        }
    }

    public boolean moveItemToCategory(String id, String category) throws IOException {
        File file = sourceFile(id);
        if (file == null) return false;

        // Preserve custom item IDs. Never move someone else's Slimefun item.
        SlimefunItem registered = SlimefunItem.getById(id);
        if (registered != null && (registered.getAddon() == null
                || !registered.getAddon().getName().equals(plugin.getName()))) return false;

        YamlConfiguration categories = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "categories.yml"));
        if (!categories.isConfigurationSection(category)
                || "nested".equalsIgnoreCase(categories.getString(category + ".type"))) {
            return false;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set(id + ".category", category);
        yaml.save(file);

        ItemGroup target = Registry.allItemGroups.get(category);
        if (registered != null && target != null
                && !(target instanceof io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup)) {
            registered.setItemGroup(target);
            applyOrder();
        }
        // If registration failed before this item/group was loaded, the saved
        // YAML takes effect on restart when registration succeeds.
        return true;
    }

    public File sourceFile(String id) {
        for (String filename : ITEM_FILES) {
            File file = new File(plugin.getDataFolder(), filename);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yaml.isConfigurationSection(id)) return file;
        }
        return null;
    }

    public boolean isMachine(String id) {
        File file = sourceFile(id);
        return file != null && file.getName().equals("machines.yml");
    }

    public YamlConfiguration load(String id) {
        File file = sourceFile(id);
        return file == null ? null : YamlConfiguration.loadConfiguration(file);
    }

    public void save(String id, YamlConfiguration config) throws IOException {
        File file = sourceFile(id);
        if (file == null) throw new IOException("No configuration source found for " + id);
        config.save(file);
    }

    public ItemStack readIngredient(YamlConfiguration yaml, String path) {
        String type = yaml.getString(path + ".type", "NONE");
        String id = yaml.getString(path + ".id", "");
        int amount = Math.max(1, yaml.getInt(path + ".amount", 1));
        ItemStack item;
        if (type.equalsIgnoreCase("SLIMEFUN")) {
            SlimefunItem sf = SlimefunItem.getById(id.toUpperCase(Locale.ROOT));
            item = sf == null ? null : sf.getItem().clone();
        } else if (type.equalsIgnoreCase("SAVEDITEM")) {
            File saved = new File(plugin.getDataFolder(), "saveditems/" + id + ".yml");
            item = YamlConfiguration.loadConfiguration(saved).getItemStack("item");
        } else if (type.equalsIgnoreCase("VANILLA")) {
            Material material = Material.matchMaterial(id);
            item = material == null || material.isAir() ? null : new ItemStack(material);
        } else {
            return null;
        }
        if (item != null) item.setAmount(Math.min(item.getMaxStackSize(), amount));
        return item;
    }

    public void writeIngredient(YamlConfiguration yaml, String path, ItemStack ingredient) throws IOException {
        if (ingredient == null || ingredient.getType().isAir()) {
            yaml.set(path + ".type", "NONE");
            yaml.set(path + ".id", "N/A");
            yaml.set(path + ".amount", 1);
            return;
        }
        if (ingredient.getAmount() < 1 || ingredient.getAmount() > ingredient.getMaxStackSize()) {
            throw new IOException("Ingredient quantity exceeds its stack limit");
        }

        SlimefunItem sf = SlimefunItem.getByItem(ingredient);
        if (sf != null) {
            yaml.set(path + ".type", "SLIMEFUN");
            yaml.set(path + ".id", sf.getId());
        } else if (ingredient.hasItemMeta()) {
            // Preserve the exact identity of Valhalla, texture-provider and
            // other custom items by saving their complete ItemStack metadata.
            String id = "GUI_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            File saved = new File(plugin.getDataFolder(), "saveditems/" + id + ".yml");
            YamlConfiguration itemFile = new YamlConfiguration();
            ItemStack template = ingredient.clone();
            template.setAmount(1);
            itemFile.set("item", template);
            itemFile.save(saved);
            yaml.set(path + ".type", "SAVEDITEM");
            yaml.set(path + ".id", id);
        } else {
            yaml.set(path + ".type", "VANILLA");
            yaml.set(path + ".id", ingredient.getType().name());
        }
        yaml.set(path + ".amount", ingredient.getAmount());
    }

    public static List<String> recipeKeys(YamlConfiguration yaml, String item) {
        if (!yaml.isConfigurationSection(item + ".recipes")) return new ArrayList<>();
        return new ArrayList<>(yaml.getConfigurationSection(item + ".recipes").getKeys(false));
    }

    public static String newMachineRecipeKey(YamlConfiguration yaml, String item) {
        Set<String> used = new HashSet<>(recipeKeys(yaml, item));
        int i = 1;
        while (used.contains(String.valueOf(i))) i++;
        return String.valueOf(i);
    }
}
