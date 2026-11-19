package io.ncbpfluffybear.slimecustomizer.editor;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.ncbpfluffybear.slimecustomizer.SlimeCustomizer;
import org.bukkit.inventory.ItemStack;

/** Applies edited output before native Slimefun recipe registration. */
public final class RecipeOutputOverride {
    private RecipeOutputOverride() {}

    public static void register(SlimefunItem item, Config config, String key) {
        if (config.getValue(key + ".recipe-output") != null) {
            ItemStack output = config.getItem(key + ".recipe-output");
            if (output == null || output.getType().isAir()) {
                throw new IllegalArgumentException("Invalid recipe-output for " + key);
            }
            item.setRecipeOutput(output);
        }
        item.register(SlimeCustomizer.getInstance());
    }
}
