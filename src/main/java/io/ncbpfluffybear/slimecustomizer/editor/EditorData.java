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

    /** Key format is namespace:key, to avoid collisions between add-ons. */
    public static String groupId(ItemGroup group) {
        return group.getKey().toString();
    }

    public ItemGroup group(String id) {
        for (ItemGroup group : io.github.thebusybiscuit.slimefun4.implementation.Slimefun
                .getRegistry().getAllItemGroups()) {
            if (groupId(group).equals(id)) return group;
        }
        if (id.startsWith("slimecustomizer:")) {
            return Registry.allItemGroups.get(id.substring("slimecustomizer:".length()));
        }
        return Registry.allItemGroups.get(id); // historical custom key
    }

    public static boolean supportsItems(ItemGroup group) {
        return group != null && !(group instanceof
                io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup);
    }

    public List<String> categories() {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        List<String> saved = YamlConfiguration.loadConfiguration(orderFile)
                .getStringList("category-order");
        found.addAll(saved);
        for (ItemGroup group : io.github.thebusybiscuit.slimefun4.implementation.Slimefun
                .getRegistry().getAllItemGroups()) {
            found.add(groupId(group));
        }

        // Include YAML categories even if a registration failure left them
        // absent from the Slimefun registry. These remain repairable in GUI.
        YamlConfiguration own = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "categories.yml"));
        for (String key : own.getKeys(false)) found.add("slimecustomizer:" + key.toLowerCase(Locale.ROOT));
        found.removeIf(id -> group(id) == null
                && !(id.startsWith("slimecustomizer:") && own.isConfigurationSection(
                id.substring("slimecustomizer:".length()))));
        return new ArrayList<>(found);
    }

    public List<String> items(String categoryId) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        ItemGroup group = group(categoryId);
        if (supportsItems(group)) {
            try {
                for (SlimefunItem item : group.getItems()) {
                    result.add(item.getId());
                }
            } catch (UnsupportedOperationException exception) {
                // A custom ItemGroup may reject enumerating its items.
                plugin.getLogger().fine("Category doesn't expose an item list: " + categoryId);
            }
        }

        // Show unregistered custom items in recovery mode. Do not pretend
        // Slimefun's FlexItemGroup has any items.
        if (group == null || supportsItems(group)) {
            for (String filename : ITEM_FILES) {
                YamlConfiguration source = YamlConfiguration.loadConfiguration(
                        new File(plugin.getDataFolder(), filename));
                for (String id : source.getKeys(false)) {
                    String destination = source.getString(id + ".category", "");
                    String canonical = destination.startsWith("existing:")
                            ? destination.substring("existing:".length())
                            : "slimecustomizer:" + destination.toLowerCase(Locale.ROOT);
                    if (categoryId.equalsIgnoreCase(canonical)) result.add(id);
                }
            }
        }

        YamlConfiguration order = YamlConfiguration.loadConfiguration(orderFile);
        List<String> configured = order.getStringList("item-order." + categoryId);
        List<String> sorted = new ArrayList<>(result);
        sorted.sort(Comparator.comparingInt(id -> index(configured, id)));
        return sorted;
    }

    /** Nested groups have child groups, not items; display them as navigation. */
    public List<String> subGroups(String parentId) {
        ItemGroup parent = group(parentId);
        if (!(parent instanceof io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup)) {
            return List.of();
        }
        List<String> children = new ArrayList<>();
        for (ItemGroup candidate : io.github.thebusybiscuit.slimefun4.implementation.Slimefun
                .getRegistry().getAllItemGroups()) {
            if (candidate instanceof io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup sub
                    && sub.getParent() == parent) children.add(groupId(sub));
        }
        return children;
    }

    private static int index(List<String> order, String id) {
        int i = order.indexOf(id);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    public boolean moveCategory(String id, int direction) throws IOException {
        List<String> list = categories();
        int from = list.indexOf(id), to = from + direction;
        if (from < 0 || to < 0 || to >= list.size()) return false;
        java.util.Collections.swap(list, from, to);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        config.set("category-order", list);
        config.save(orderFile);
        applyOrder();
        return true;
    }

    public boolean moveItem(String categoryId, String id, int direction) throws IOException {
        ItemGroup group = group(categoryId);
        if (group != null && !supportsItems(group)) return false;
        List<String> current = items(categoryId);
        int from = current.indexOf(id), to = from + direction;
        if (from < 0 || to < 0 || to >= current.size()) return false;
        java.util.Collections.swap(current, from, to);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        config.set("item-order." + categoryId, current);
        config.save(orderFile);
        applyOrder();
        return true;
    }

    public void applyOrder() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(orderFile);
        // Apply optional visual-only placements of third-party/Slimefun core
        // items; their recipe, ID, addon and research registration are intact.
        if (config.isConfigurationSection("category-override")) {
            for (String itemId : config.getConfigurationSection("category-override").getKeys(false)) {
                String targetKey = config.getString("category-override." + itemId);
                SlimefunItem item = SlimefunItem.getById(itemId);
                ItemGroup target = group(targetKey);
                if (item != null && supportsItems(target) && item.getItemGroup() != target) {
                    try {
                        item.setItemGroup(target);
                    } catch (RuntimeException error) {
                        plugin.getLogger().warning("Could not move item " + itemId + ": " + error.getMessage());
                    }
                }
            }
        }

        List<ItemGroup> registry = io.github.thebusybiscuit.slimefun4.implementation.Slimefun
                .getRegistry().getAllItemGroups();
        List<String> desiredCategories = config.getStringList("category-order");
        if (!desiredCategories.isEmpty()) {
            // Sorting the actual registry list, not changing built-in Slimefun
            // tiers, preserves the original category keys and relationships.
            registry.sort(Comparator.comparingInt(g -> index(desiredCategories, groupId(g))));
        }

        for (ItemGroup group : registry) {
            if (!supportsItems(group)) continue; // FlexItemGroup#getItems throws.
            String key = groupId(group);
            List<String> order = config.getStringList("item-order." + key);
            if (!order.isEmpty()) {
                try {
                    group.getItems().sort(Comparator.comparingInt(i -> index(order, i.getId())));
                } catch (UnsupportedOperationException exception) {
                    plugin.getLogger().fine("Item ordering not supported by " + key);
                }
            }
        }
    }

    public boolean moveItemToCategory(String itemId, String categoryId) throws IOException {
        ItemGroup target = group(categoryId);
        if (!supportsItems(target)) return false;
        SlimefunItem item = SlimefunItem.getById(itemId);
        File source = sourceFile(itemId);

        if (source != null) {
            // Save SlimeCustomizer-owned changes to the original item YAML.
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(source);
            String value = "slimecustomizer".equals(target.getKey().getNamespace())
                    ? target.getKey().getKey()
                    : "existing:" + groupId(target);
            yaml.set(itemId + ".category", value);
            yaml.save(source);
        } else if (item != null) {
            // Third-party item placement belongs to editor-order.yml, not its
            // owning addon's config. Only visual location is overridden.
            YamlConfiguration order = YamlConfiguration.loadConfiguration(orderFile);
            order.set("category-override." + itemId, categoryId);
            order.save(orderFile);
        } else return false;

        if (item != null && item.getItemGroup() != target) item.setItemGroup(target);
        applyOrder();
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
