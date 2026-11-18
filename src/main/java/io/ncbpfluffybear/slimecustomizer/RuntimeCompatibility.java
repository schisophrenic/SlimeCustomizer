package io.ncbpfluffybear.slimecustomizer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Small, fail-closed startup check for the supported Slimefun-Legacy server family.
 * No item IDs, recipes or persistent block data are modified here.
 */
public final class RuntimeCompatibility {

    private RuntimeCompatibility() {}

    public static boolean check(JavaPlugin addon) {
        String minecraft = Bukkit.getMinecraftVersion();
        String serverName = Bukkit.getName();
        String normalizedServer = serverName.toLowerCase(Locale.ROOT);
        boolean paperFamily = normalizedServer.contains("paper") || normalizedServer.contains("purpur");
        boolean mc12111 = minecraft.equals("1.21.11");
        boolean mc26x = minecraft.matches("26\\.[123](\\..*)?");
        int requiredJava = mc26x ? 25 : 21;
        int actualJava = Runtime.version().feature();

        if (!paperFamily) {
            addon.getLogger().severe("Unsupported server platform: " + serverName
                    + ". Supported: Paper or Purpur.");
            return false;
        }
        if (!mc12111 && !mc26x) {
            addon.getLogger().severe("Unsupported Minecraft version: " + minecraft
                    + ". Supported: 1.21.11, 26.1, 26.2 and 26.3.");
            return false;
        }
        if (actualJava < requiredJava) {
            addon.getLogger().severe("Minecraft " + minecraft + " requires Java "
                    + requiredJava + "+; detected Java " + actualJava + ".");
            return false;
        }

        Plugin slimefun = Bukkit.getPluginManager().getPlugin("Slimefun");
        if (slimefun == null || !slimefun.isEnabled()) {
            addon.getLogger().severe("Slimefun-Legacy is missing or disabled.");
            return false;
        }

        try {
            // This API is implemented by the maintained Slimefun-Legacy fork.
            Class.forName("io.github.thebusybiscuit.slimefun4.api.addons.AddonCompatibilityService",
                    false, slimefun.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError error) {
            addon.getLogger().severe("This Slimefun installation does not expose the"
                    + " Slimefun-Legacy compatibility API. Do not use the original Slimefun4 core.");
            return false;
        }

        addon.getLogger().info("Compatibility preflight passed: " + serverName + " "
                + minecraft + ", Java " + actualJava + ", Slimefun " + slimefun.getPluginMeta().getVersion()
                + " (integration still requires gameplay testing).");
        return true;
    }
}
