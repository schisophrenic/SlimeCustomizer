# SlimeCustomizer GUI Editor and ValhallaMMO Bridge (testing)

This branch extends the tested Java 21 / Paper 1.21.11 and 26.3 build.
Install only the correct runnable JAR from the new CI run on a backed-up test
server. Existing Slimefun item IDs, normal crafting recipes and machine state
are not migrated or overwritten by opening the editor.

## Open the GUI

Run `/sc editor` in game with the `slimecustomizer.admin` permission.

### Categories and items

* Click a category to browse its SlimeCustomizer items.
* Shift-left a category to move it up; shift-right to move it down.
* The GUI writes ordering to `editor-order.yml`. It never renames item IDs.
* Categories use distinct Slimefun tiers after you reorder them. This moves
  custom categories into the 100+ guide tier range, leaving other add-ons
  unchanged.
* In each category, shift-left/right on an item to change its order.
* Click an item for its crafting grid. Click **Move to another category** to
  move a SlimeCustomizer item to a different custom category. Nested parent
  categories are not valid item destinations. This updates the item's
  existing YAML and its current Slimefun item group.
* In a machine category, right-click a machine to edit its processing recipes.

### Crafting GUI

* Grid positions are real 3x3 recipe positions, but **ghost displays**: cursor
  + left click copies an ingredient and its current stack amount; an empty
  cursor + left click clears it. Right-click increases the required amount,
  shift-right decreases it. Nothing is deducted from the cursor or inventory.
* The result slot accepts another exact ItemStack. This updates the actual
  recipe result without changing the registered Slimefun item identity.
* Non-vanilla items are saved to `saveditems/GUI_*.yml` with complete item
  metadata; Slimefun items are recognized by their real registered IDs.
* Standard Slimefun enhanced crafting/armor forge/magic workbench/pressure
  chamber still limit quantities to **one per grid slot**. In those cases,
  enable **Valhalla grid mode** to use per-slot quantities >1.
* Regular `items.yml` item recipes can toggle Valhalla grid mode from the
  GUI. The optional `VALHALLA_GRID` recipe type delegates matching and
  consumption to ValhallaMMO-Rewritten's DynamicGridRecipe. It uses
  `ExactChoice` for **every non-empty slot**, matching custom PDC/meta
  instead of just vanilla material. It never registers the same craft in the
  Slimefun crafting machine, preventing duplicate consumption or experience.
* The GUI's Valhalla output multiplier is a configurable fixed x1..x16 on
  the registered Valhalla result. It must fit in a stack. This does **not**
  directly modify Valhalla skill perks or dynamically multiply them. Any
  existing Valhalla crafting modifiers still run through Valhalla's own
  recipe processing system.

### Machine processing GUI

* From a machine, use **Machine process recipes** to see, edit, add or delete
  processing recipes.
* Each process supports two exact input slots, two output slots, per-slot
  quantities and the processing time in seconds.
* A newly added process is saved to the existing `machines.yml` format with
  a unique recipe key. Deletion requires two clicks.
* Machine operations are not hot-reloaded. **Restart** the test server after
  changing any crafting or processing recipe. Category and item ordering
  changes appear immediately and persist between restarts.

### Compatibility restrictions for first testing

* Valhalla grid recipes require `ValhallaMMO` (Rewritten branch's recipe
  classes) to be installed and enabled. If absent, the rest of SlimeCustomizer
  can run, but `VALHALLA_GRID` items cannot craft.
* The adapter does not load ValhallaMMO's JAR into SlimeCustomizer and avoids
  compile-time dependency. It creates Valhalla DynamicGridRecipe, SlotEntry and
  ExactChoice objects through the installed Valhalla plugin's classloader and
  persists them in Valhalla's own recipe registry.
* A normal Slimefun crafting recipe and Valhalla grid recipe are **mutually
  exclusive** for each edited item.
* Test normal click and shift-click crafting with four diamonds in one slot,
  custom Slimefun ingredients, Valhalla custom items, output counts, no
  duplication, and recipe persistence after a restart.
* Reordering and GUI YAML editing have not yet been tested on a live
  Purpur server. This is a testing build; do not use on production.

