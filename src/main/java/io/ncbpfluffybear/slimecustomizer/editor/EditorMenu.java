package io.ncbpfluffybear.slimecustomizer.editor;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.ncbpfluffybear.slimecustomizer.Registry;
import io.ncbpfluffybear.slimecustomizer.SlimeCustomizer;
import io.ncbpfluffybear.slimecustomizer.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Admin-only inventory GUI. Editor icons are never transferable inventory items.
 * Inputs are ghost samples from the player's cursor, so the editor never
 * consumes, drops, duplicates, or changes items in real inventories.
 */
public final class EditorMenu implements Listener {
    private static final int[] CRAFT_GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] PROCESS_INPUT = {11, 12};
    private static final int[] PROCESS_OUTPUT = {24, 25};
    private static final int PAGE_SIZE = 45;

    private enum Page { CATEGORIES, ITEMS, MOVE, CRAFT, MACHINE_LIST, PROCESS }

    private final SlimeCustomizer plugin;
    private final EditorData data;

    public EditorMenu(SlimeCustomizer plugin) {
        this.plugin = plugin;
        this.data = new EditorData(plugin);
    }

    public void initialize() {
        data.applyOrder();
    }

    public void open(Player player) {
        if (!player.hasPermission("slimecustomizer.admin")) {
            Utils.send(player, "&cYou need slimecustomizer.admin.");
            return;
        }
        Session s = new Session(player);
        show(s, Page.CATEGORIES);
    }

    private final class Session implements InventoryHolder {
        private final Player player;
        private Inventory inventory;
        private Page page;
        private int pageNumber;
        private String category;
        private String itemId;
        private String machineRecipeId;
        private boolean newMachineRecipe;
        private Draft draft;
        private boolean confirmDelete;

        private Session(Player player) {
            this.player = player;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Draft {
        ItemStack[] inputs;
        ItemStack[] outputs;
        String nativeRecipeType;
        boolean valhalla;
        int multiplier;
        int seconds;
        boolean modified;

        Draft(int in, int out) {
            inputs = new ItemStack[in];
            outputs = new ItemStack[out];
            multiplier = 1;
            seconds = 5;
        }
    }

    private void show(Session s, Page page) {
        s.page = page;
        String title = switch (page) {
            case CATEGORIES -> "SC Editor: Categories";
            case ITEMS -> "SC Editor: " + trim(s.category, 19);
            case MOVE -> "Move item to category";
            case CRAFT -> "Crafting: " + trim(s.itemId, 18);
            case MACHINE_LIST -> "Machine recipes: " + trim(s.itemId, 14);
            case PROCESS -> "Processing: " + trim(s.machineRecipeId, 18);
        };
        s.inventory = Bukkit.createInventory(s, 54, title);
        render(s);
        s.player.openInventory(s.inventory);
    }

    private void render(Session s) {
        Inventory inv = s.inventory;
        inv.clear();
        switch (s.page) {
            case CATEGORIES -> {
                List<String> categories = data.categories();
                renderList(s, categories, true);
                inv.setItem(49, button(Material.BOOK, "&eCategory ordering",
                        "&7Shift-left/right on a category", "&7to move it up/down in the Slimefun Guide.",
                        "&7Only SlimeCustomizer categories are changed."));
            }
            case ITEMS -> {
                renderList(s, data.items(s.category), false);
                inv.setItem(49, button(Material.BOOK, "&eItem order",
                        "&7Left: crafting recipe", "&7Right: machine processing recipes",
                        "&7Shift-left/right: move item up/down", "&7Moving items to another category is",
                        "&7available inside the crafting editor."));
            }
            case MOVE -> {
                renderList(s, data.categories(), true);
                inv.setItem(49, button(Material.CHEST, "&eChoose a destination",
                        "&7Click a custom category to move this item there.",
                        "&7Nested parent categories are excluded."));
            }
            case MACHINE_LIST -> {
                YamlConfiguration yaml = data.load(s.itemId);
                List<String> recipes = yaml == null ? List.of() : EditorData.recipeKeys(yaml, s.itemId);
                int start = s.pageNumber * PAGE_SIZE;
                for (int i = start; i < Math.min(recipes.size(), start + PAGE_SIZE); i++) {
                    String id = recipes.get(i);
                    String p = s.itemId + ".recipes." + id + ".output.1";
                    ItemStack output = data.readIngredient(yaml, p);
                    inv.setItem(i - start, button(output == null ? new ItemStack(Material.FURNACE) : output,
                            "&eProcess " + id, "&7Left-click to edit input/output and speed"));
                }
                navigation(s, recipes.size());
                inv.setItem(45, button(Material.LIME_DYE, "&aAdd new processing recipe",
                        "&7Create a new process in this machine.",
                        "&7Ingredients, results and speed are edited in the GUI."));
                inv.setItem(49, button(Material.BOOK, "&eMachine recipes",
                        "&7Up to two exact inputs and two outputs.",
                        "&7Click to edit, save, then restart."));
            }
            case CRAFT -> renderCraft(s);
            case PROCESS -> renderProcess(s);
        }
        if (s.page != Page.CATEGORIES && s.page != Page.CRAFT && s.page != Page.PROCESS) {
            inv.setItem(46, button(Material.ARROW, "&eBack"));
        }
    }

    private void renderList(Session s, List<String> list, boolean categories) {
        int start = s.pageNumber * PAGE_SIZE;
        for (int i = start; i < Math.min(list.size(), start + PAGE_SIZE); i++) {
            String id = list.get(i);
            ItemStack base;
            if (categories) {
                ItemGroup group = Registry.allItemGroups.get(id);
                base = group == null ? null : group.getItem(s.player);
            } else {
                SlimefunItem sf = SlimefunItem.getById(id);
                base = sf == null ? null : sf.getItem();
            }
            s.inventory.setItem(i - start, button(base == null ? new ItemStack(Material.PAPER) : base,
                    "&e" + id, categories
                            ? "&7Left: open category   Shift-left/right: reorder"
                            : "&7Left: edit crafting   Right: machine processes",
                    categories ? "&7Right-click: open category"
                            : "&7Shift-left/right: reorder this item"));
        }
        navigation(s, list.size());
    }

    private void navigation(Session s, int size) {
        if (s.pageNumber > 0) {
            s.inventory.setItem(48, button(Material.ARROW, "&ePrevious page"));
        }
        if ((s.pageNumber + 1) * PAGE_SIZE < size) {
            s.inventory.setItem(50, button(Material.ARROW, "&eNext page"));
        }
    }

    private void renderCraft(Session s) {
        Draft d = s.draft;
        for (int i = 0; i < CRAFT_GRID.length; i++) {
            s.inventory.setItem(CRAFT_GRID[i], preview(d.inputs[i], "&7Slot " + (i + 1)));
        }
        s.inventory.setItem(24, preview(d.outputs[0], "&aResult"));
        s.inventory.setItem(39, button(Material.CHEST, "&eMove to another category",
                "&7Choose a SlimeCustomizer category."));
        if (data.isMachine(s.itemId)) {
            s.inventory.setItem(40, button(Material.FURNACE, "&eMachine process recipes",
                    "&7Edit/add processing recipes for this machine."));
        } else if (new java.io.File(plugin.getDataFolder(), "items.yml").equals(data.sourceFile(s.itemId))) {
            s.inventory.setItem(40, button(Material.CRAFTING_TABLE,
                    d.valhalla ? "&aValhalla grid mode: ON" : "&7Valhalla grid mode: OFF",
                    "&7Click to switch recipe engine.", "&7Valhalla mode supports quantities above 1",
                    "&7and exact Slimefun/Valhalla item matching.",
                    "&7Requires ValhallaMMO-Rewritten."));
            s.inventory.setItem(41, button(Material.EXPERIENCE_BOTTLE, "&eValhalla output multiplier: x" + d.multiplier,
                    "&7Right +1  Shift-right -1 (1 to 16)", "&7Applied when Valhalla grid mode is ON.",
                    "&7No separate crafting event or duplicate rewards."));
        }
        s.inventory.setItem(45, button(Material.ARROW, "&eBack to items"));
        s.inventory.setItem(49, button(Material.EMERALD, "&aSAVE recipe",
                "&7Save YAML and restart to activate.",
                "&7Cursor + left on grid: copy exact item + stack size",
                "&7Empty cursor + left: clear", "&7Right: amount +1",
                "&7Shift-right: amount -1", "&7All slots are ghost previews only."));
        s.inventory.setItem(53, button(Material.PAPER, "&eEditing " + s.itemId,
                d.valhalla ? "&7Crafting through Valhalla grid" : "&7Crafting through Slimefun",
                "&7Existing item ID and NBT stay unchanged."));
    }

    private void renderProcess(Session s) {
        Draft d = s.draft;
        for (int i = 0; i < 2; i++) {
            s.inventory.setItem(PROCESS_INPUT[i], preview(d.inputs[i], "&bInput " + (i + 1)));
            s.inventory.setItem(PROCESS_OUTPUT[i], preview(d.outputs[i], "&aOutput " + (i + 1)));
        }
        s.inventory.setItem(40, button(Material.CLOCK, "&eTime: " + d.seconds + " seconds",
                "&7Right: +1 second  Shift-right: -1 second", "&7Minimum 1 second"));
        s.inventory.setItem(45, button(Material.ARROW, "&eBack to machine recipes"));
        s.inventory.setItem(49, button(Material.EMERALD, "&aSAVE processing recipe",
                "&7Use a real item on your cursor to set an ingredient.",
                "&7Empty cursor + left clears; right edits quantity.",
                "&7Both inputs and outputs use exact metadata.",
                "&7Requires restart to activate."));
        s.inventory.setItem(53, button(Material.BARRIER, s.confirmDelete
                        ? "&cCONFIRM: click again to delete" : "&cDelete processing recipe",
                "&7Click twice to remove this recipe from YAML.",
                "&7Requires restart."));
    }

    private ItemStack preview(ItemStack item, String title) {
        if (item == null) return button(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "&8" + title + " (empty)", "&7Cursor + left: set item", "&7Right: increase amount");
        return button(item, title + " &ex" + item.getAmount(),
                "&7Cursor + left: replace item / quantity",
                "&7Empty cursor + left: clear",
                "&7Right: +1   Shift-right: -1");
    }

    private static ItemStack button(Material material, String title, String... lines) {
        return button(new ItemStack(material), title, lines);
    }

    private static ItemStack button(ItemStack template, String title, String... lines) {
        ItemStack item = template.clone();
        item.setAmount(1); // GUI icon only; quantity is represented in lore/title.
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.color(title));
            List<String> lore = new ArrayList<>();
            for (String s : lines) lore.add(Utils.color(s));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String trim(String value, int size) {
        if (value == null) return "";
        return value.length() <= size ? value : value.substring(0, size);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Session s)) return;
        // Never allow shift-click, hotbar swaps or collect-to-cursor to send
        // a player item into an admin-only ghost inventory.
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK
                    || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (!s.player.hasPermission("slimecustomizer.admin")) {
            s.player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        try {
            switch (s.page) {
                case CATEGORIES -> categoriesClick(s, slot, click);
                case ITEMS -> itemsClick(s, slot, click);
                case MOVE -> moveClick(s, slot);
                case CRAFT -> craftClick(s, slot, click, event.getCursor());
                case MACHINE_LIST -> machineListClick(s, slot);
                case PROCESS -> processClick(s, slot, click, event.getCursor());
            }
        } catch (IOException | IllegalArgumentException error) {
            plugin.getLogger().log(Level.WARNING, "Editor action failed", error);
            Utils.send(s.player, "&cEditor action failed: " + error.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Session)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private void categoriesClick(Session s, int slot, ClickType click) throws IOException {
        if (pageButton(s, slot, data.categories().size())) return;
        List<String> ids = data.categories();
        int selected = s.pageNumber * PAGE_SIZE + slot;
        if (selected < 0 || selected >= ids.size() || slot >= PAGE_SIZE) return;
        String id = ids.get(selected);
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            data.moveCategory(id, click == ClickType.SHIFT_LEFT ? -1 : 1);
            render(s);
        } else {
            s.category = id;
            s.pageNumber = 0;
            show(s, Page.ITEMS);
        }
    }

    private void itemsClick(Session s, int slot, ClickType click) throws IOException {
        if (slot == 46) {
            s.pageNumber = 0;
            show(s, Page.CATEGORIES);
            return;
        }
        if (pageButton(s, slot, data.items(s.category).size())) return;
        List<String> items = data.items(s.category);
        int selected = s.pageNumber * PAGE_SIZE + slot;
        if (selected < 0 || selected >= items.size() || slot >= PAGE_SIZE) return;
        String id = items.get(selected);
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            data.moveItem(s.category, id, click == ClickType.SHIFT_LEFT ? -1 : 1);
            render(s);
            return;
        }
        s.itemId = id;
        s.pageNumber = 0;
        if (click.isRightClick() && data.isMachine(id)) {
            show(s, Page.MACHINE_LIST);
        } else {
            s.draft = craftingDraft(id);
            show(s, Page.CRAFT);
        }
    }

    private void moveClick(Session s, int slot) throws IOException {
        if (slot == 46) {
            s.draft = craftingDraft(s.itemId);
            show(s, Page.CRAFT);
            return;
        }
        List<String> groups = data.categories();
        if (pageButton(s, slot, groups.size())) return;
        int index = s.pageNumber * PAGE_SIZE + slot;
        if (index < 0 || index >= groups.size() || slot >= PAGE_SIZE) return;
        if (data.moveItemToCategory(s.itemId, groups.get(index))) {
            s.category = groups.get(index);
            Utils.send(s.player, "&aMoved item to " + s.category + " (saved).");
        } else {
            Utils.send(s.player, "&cThis category cannot receive items.");
        }
        s.draft = craftingDraft(s.itemId);
        show(s, Page.CRAFT);
    }

    private void machineListClick(Session s, int slot) {
        if (slot == 46) {
            show(s, Page.ITEMS);
            return;
        }
        if (slot == 45) {
            YamlConfiguration config = data.load(s.itemId);
            s.machineRecipeId = EditorData.newMachineRecipeKey(config, s.itemId);
            s.newMachineRecipe = true;
            s.confirmDelete = false;
            s.draft = new Draft(2, 2);
            show(s, Page.PROCESS);
            return;
        }
        YamlConfiguration yaml = data.load(s.itemId);
        List<String> ids = EditorData.recipeKeys(yaml, s.itemId);
        if (pageButton(s, slot, ids.size())) return;
        int i = s.pageNumber * PAGE_SIZE + slot;
        if (i < 0 || i >= ids.size() || slot >= PAGE_SIZE) return;
        s.machineRecipeId = ids.get(i);
        s.newMachineRecipe = false;
        s.confirmDelete = false;
        s.draft = processDraft(s.itemId, s.machineRecipeId);
        show(s, Page.PROCESS);
    }

    private boolean pageButton(Session s, int slot, int size) {
        if (slot == 48 && s.pageNumber > 0) {
            s.pageNumber--;
            render(s);
            return true;
        }
        if (slot == 50 && (s.pageNumber + 1) * PAGE_SIZE < size) {
            s.pageNumber++;
            render(s);
            return true;
        }
        return false;
    }

    private void craftClick(Session s, int slot, ClickType click, ItemStack cursor) throws IOException {
        for (int i = 0; i < CRAFT_GRID.length; i++) {
            if (CRAFT_GRID[i] == slot) {
                change(s.draft.inputs, i, click, cursor);
                render(s);
                return;
            }
        }
        if (slot == 24) {
            change(s.draft.outputs, 0, click, cursor);
            render(s);
        } else if (slot == 45) {
            show(s, Page.ITEMS);
        } else if (slot == 39) {
            s.pageNumber = 0;
            show(s, Page.MOVE);
        } else if (slot == 40 && data.isMachine(s.itemId)) {
            s.pageNumber = 0;
            show(s, Page.MACHINE_LIST);
        } else if (slot == 40 && isRegularItem(s.itemId)) {
            if (!s.draft.valhalla && !ValhallaBridge.available()) {
                Utils.send(s.player, "&cInstall ValhallaMMO-Rewritten to enable Valhalla grid recipes.");
            } else {
                s.draft.valhalla = !s.draft.valhalla;
                s.draft.modified = true;
            }
            render(s);
        } else if (slot == 41 && isRegularItem(s.itemId)) {
            s.draft.multiplier = clamp(s.draft.multiplier + (click.isShiftClick() ? -1 : 1), 1, 16);
            s.draft.modified = true;
            render(s);
        } else if (slot == 49) {
            saveCraft(s);
        }
    }

    private boolean isRegularItem(String id) {
        FileNameHelper helper = new FileNameHelper(data.sourceFile(id));
        return helper.is("items.yml");
    }

    private static final class FileNameHelper {
        private final java.io.File file;
        FileNameHelper(java.io.File file) { this.file = file; }
        boolean is(String name) { return file != null && name.equals(file.getName()); }
    }

    private void processClick(Session s, int slot, ClickType click, ItemStack cursor) throws IOException {
        for (int i = 0; i < 2; i++) {
            if (slot == PROCESS_INPUT[i]) {
                change(s.draft.inputs, i, click, cursor);
                render(s);
                return;
            }
            if (slot == PROCESS_OUTPUT[i]) {
                change(s.draft.outputs, i, click, cursor);
                render(s);
                return;
            }
        }
        if (slot == 40) {
            s.draft.seconds = clamp(s.draft.seconds + (click.isShiftClick() ? -1 : 1), 1, 3600);
            s.draft.modified = true;
            render(s);
        } else if (slot == 45) {
            s.pageNumber = 0;
            show(s, Page.MACHINE_LIST);
        } else if (slot == 49) {
            saveProcess(s);
        } else if (slot == 53) {
            if (s.newMachineRecipe) {
                Utils.send(s.player, "&cThis new recipe has not been saved.");
                show(s, Page.MACHINE_LIST);
            } else if (!s.confirmDelete) {
                s.confirmDelete = true;
                render(s);
            } else {
                YamlConfiguration config = data.load(s.itemId);
                config.set(s.itemId + ".recipes." + s.machineRecipeId, null);
                data.save(s.itemId, config);
                Utils.send(s.player, "&aDeleted process " + s.machineRecipeId + ". Restart to apply.");
                show(s, Page.MACHINE_LIST);
            }
        }
    }

    private void change(ItemStack[] slots, int index, ClickType click, ItemStack cursor) {
        ItemStack existing = slots[index];
        if (click.isLeftClick() && cursor != null && !cursor.getType().isAir()) {
            slots[index] = cursor.clone();
        } else if (click.isLeftClick()) {
            slots[index] = null;
        } else if (click.isRightClick() && existing != null) {
            ItemStack modified = existing.clone();
            int increment = click.isShiftClick() ? -1 : 1;
            modified.setAmount(clamp(modified.getAmount() + increment, 1, modified.getMaxStackSize()));
            slots[index] = modified;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Draft craftingDraft(String id) {
        YamlConfiguration yaml = data.load(id);
        Draft draft = new Draft(9, 1);
        if (yaml == null) return draft;
        String path = id + ".crafting-recipe";
        for (int i = 0; i < 9; i++) {
            draft.inputs[i] = data.readIngredient(yaml, path + "." + (i + 1));
        }
        SlimefunItem item = SlimefunItem.getById(id);
        ItemStack configuredOutput = yaml.getItemStack(id + ".recipe-output");
        draft.outputs[0] = configuredOutput == null
                ? (item == null ? null : item.getRecipeOutput().clone())
                : configuredOutput.clone();
        String type = yaml.getString(id + ".crafting-recipe-type", "ENHANCED_CRAFTING_TABLE");
        draft.valhalla = type.equalsIgnoreCase("VALHALLA_GRID");
        draft.nativeRecipeType = yaml.getString(id + ".previous-recipe-type",
                draft.valhalla ? "ENHANCED_CRAFTING_TABLE" : type);
        draft.multiplier = clamp(yaml.getInt(id + ".valhalla-output-multiplier", 1), 1, 16);
        return draft;
    }

    private Draft processDraft(String id, String recipeId) {
        YamlConfiguration yaml = data.load(id);
        Draft draft = new Draft(2, 2);
        if (yaml == null) return draft;
        String path = id + ".recipes." + recipeId;
        for (int i = 0; i < 2; i++) {
            draft.inputs[i] = data.readIngredient(yaml, path + ".input." + (i + 1));
            draft.outputs[i] = data.readIngredient(yaml, path + ".output." + (i + 1));
        }
        draft.seconds = Math.max(1, yaml.getInt(path + ".speed-in-seconds", 5));
        return draft;
    }

    private void saveCraft(Session s) throws IOException {
        Draft draft = s.draft;
        if (Arrays.stream(draft.inputs).noneMatch(i -> i != null) || draft.outputs[0] == null) {
            Utils.send(s.player, "&cCrafting requires at least one ingredient and an output.");
            return;
        }
        if (!draft.valhalla) {
            String type = draft.nativeRecipeType;
            if (List.of("ENHANCED_CRAFTING_TABLE", "MAGIC_WORKBENCH", "ARMOR_FORGE", "PRESSURE_CHAMBER").contains(type)
                    && Arrays.stream(draft.inputs).anyMatch(i -> i != null && i.getAmount() > 1)) {
                Utils.send(s.player, "&cThis Slimefun crafting type only supports 1 per slot. Enable Valhalla mode.");
                return;
            }
        }
        int resultCount = draft.outputs[0].getAmount() * (draft.valhalla ? draft.multiplier : 1);
        if (resultCount > draft.outputs[0].getMaxStackSize()) {
            Utils.send(s.player, "&cResult x multiplier exceeds maximum stack size.");
            return;
        }

        YamlConfiguration yaml = data.load(s.itemId);
        if (yaml == null) throw new IOException("Source configuration not found");
        String path = s.itemId + ".crafting-recipe";
        for (int i = 0; i < 9; i++) {
            data.writeIngredient(yaml, path + "." + (i + 1), draft.inputs[i]);
        }
        yaml.set(s.itemId + ".recipe-output", draft.outputs[0]);
        if (isRegularItem(s.itemId)) {
            yaml.set(s.itemId + ".crafting-recipe-type",
                    draft.valhalla ? "VALHALLA_GRID" : draft.nativeRecipeType);
            yaml.set(s.itemId + ".previous-recipe-type", draft.nativeRecipeType);
            yaml.set(s.itemId + ".valhalla-output-multiplier", draft.multiplier);
        }
        data.save(s.itemId, yaml);
        Utils.send(s.player, "&aSaved " + s.itemId + ". Restart to apply crafting changes.");
    }

    private void saveProcess(Session s) throws IOException {
        Draft d = s.draft;
        if (d.inputs[0] == null || d.outputs[0] == null) {
            Utils.send(s.player, "&cProcessing requires a first input and first output.");
            return;
        }
        if (d.seconds < 1) {
            Utils.send(s.player, "&cProcess time must be at least one second.");
            return;
        }
        YamlConfiguration yaml = data.load(s.itemId);
        if (yaml == null) throw new IOException("Source configuration not found");
        String path = s.itemId + ".recipes." + s.machineRecipeId;
        yaml.set(path + ".speed-in-seconds", d.seconds);
        for (int i = 0; i < 2; i++) {
            data.writeIngredient(yaml, path + ".input." + (i + 1), d.inputs[i]);
            data.writeIngredient(yaml, path + ".output." + (i + 1), d.outputs[i]);
        }
        data.save(s.itemId, yaml);
        s.newMachineRecipe = false;
        Utils.send(s.player, "&aSaved process " + s.machineRecipeId + ". Restart to apply.");
    }
}
