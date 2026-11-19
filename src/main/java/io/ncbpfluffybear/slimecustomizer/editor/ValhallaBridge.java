package io.ncbpfluffybear.slimecustomizer.editor;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.ncbpfluffybear.slimecustomizer.SlimeCustomizer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Optional adapter to the actual ValhallaMMO-Rewritten dynamic grid registry.
 * No Valhalla classes are linked at compile time; missing/old versions leave
 * SlimeCustomizer's native recipes and machines unaffected.
 *
 * In Valhalla grid mode, Slimefun's own recipe type is NULL, preventing two
 * systems from trying to consume the same craft. The Valhalla transaction
 * handles quantity requirements, exact metadata, and shift-click validation.
 */
public final class ValhallaBridge {
    private ValhallaBridge() {}

    public static boolean available() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ValhallaMMO");
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            Class.forName("me.athlaeos.valhallammo.crafting.recipetypes.DynamicGridRecipe",
                    false, plugin.getClass().getClassLoader());
            Class.forName("me.athlaeos.valhallammo.crafting.ingredientconfiguration.implementations.ExactChoice",
                    false, plugin.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static void start(SlimeCustomizer addon) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new java.io.File(addon.getDataFolder(), "items.yml"));
        boolean configured = yaml.getKeys(false).stream().anyMatch(
                id -> "VALHALLA_GRID".equalsIgnoreCase(yaml.getString(id + ".crafting-recipe-type", "")));
        if (!configured) return;
        if (!available()) {
            addon.getLogger().warning("Valhalla grid recipes are configured, but ValhallaMMO-Rewritten "
                    + "is not available. These recipes will not craft until that plugin is installed.");
            return;
        }

        // The Valhalla recipe JSON loader schedules registrations asynchronously.
        // Register customizer recipes on the server thread after its startup pass.
        Bukkit.getScheduler().runTaskLater(addon, () -> {
            EditorData data = new EditorData(addon);
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                    new java.io.File(addon.getDataFolder(), "items.yml"));
            int successes = 0;
            for (String id : configuration.getKeys(false)) {
                if (!"VALHALLA_GRID".equalsIgnoreCase(
                        configuration.getString(id + ".crafting-recipe-type", ""))) continue;
                try {
                    register(addon, data, configuration, id);
                    successes++;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    addon.getLogger().log(Level.SEVERE,
                            "Could not register Valhalla grid recipe for " + id
                                    + ". Existing native Slimefun recipes are unaffected.", e);
                }
            }
            addon.getLogger().info("Valhalla grid recipes registered: " + successes);
        }, 60L);
    }

    private static void register(SlimeCustomizer plugin, EditorData data,
                                 YamlConfiguration yaml, String itemId)
            throws ReflectiveOperationException {
        Plugin val = Bukkit.getPluginManager().getPlugin("ValhallaMMO");
        if (val == null || !val.isEnabled()) throw new IllegalStateException("ValhallaMMO disabled");
        ClassLoader loader = val.getClass().getClassLoader();
        String root = "me.athlaeos.valhallammo.crafting.";
        Class<?> gridClass = Class.forName(root + "recipetypes.DynamicGridRecipe", true, loader);
        Class<?> slotClass = Class.forName(root + "ingredientconfiguration.SlotEntry", true, loader);
        Class<?> choiceInterface = Class.forName(root + "ingredientconfiguration.IngredientChoice", true, loader);
        Class<?> exactClass = Class.forName(
                root + "ingredientconfiguration.implementations.ExactChoice", true, loader);
        Class<?> registry = Class.forName(root + "CustomRecipeRegistry", true, loader);

        SlimefunItem item = SlimefunItem.getById(itemId);
        if (item == null) throw new IllegalStateException("Slimefun item " + itemId + " is missing");

        String name = "slimecustomizer_" + itemId.toLowerCase(Locale.ROOT);
        Object recipe = gridClass.getConstructor(String.class).newInstance(name);
        Constructor<?> slotConstructor = slotClass.getConstructor(ItemStack.class, choiceInterface);
        Map<Integer, Object> contents = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack ingredient = data.readIngredient(yaml,
                    itemId + ".crafting-recipe." + (i + 1));
            if (ingredient == null) continue;
            Object exact = exactClass.getConstructor().newInstance();
            contents.put(i, slotConstructor.newInstance(ingredient.clone(), exact));
        }
        if (contents.isEmpty()) throw new IllegalStateException("No ingredients");
        ItemStack result = item.getRecipeOutput().clone();
        int multiple = yaml.getInt(itemId + ".valhalla-output-multiplier", 1);
        if (multiple < 1 || multiple > 16 || result.getAmount() > result.getMaxStackSize() / multiple) {
            throw new IllegalStateException("Output multiplier exceeds stack limit");
        }
        result.setAmount(result.getAmount() * multiple);

        gridClass.getMethod("setItems", Map.class).invoke(recipe, contents);
        gridClass.getMethod("setResult", ItemStack.class).invoke(recipe, result);
        gridClass.getMethod("setUnlockedForEveryone", boolean.class).invoke(recipe, true);

        Method getExisting = registry.getMethod("getGridRecipes");
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) getExisting.invoke(null);
        if (existing.containsKey(name)) {
            registry.getMethod("unregister", String.class).invoke(null, name);
        }
        registry.getMethod("register", gridClass, boolean.class).invoke(null, recipe, false);
        registry.getMethod("setChangesMade").invoke(null);
        registry.getMethod("saveRecipes", boolean.class).invoke(null, false);
    }
}
