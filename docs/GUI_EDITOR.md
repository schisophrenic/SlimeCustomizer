# In-game GUI editor

Open `/sc editor` as an operator or with `slimecustomizer.admin`.
It includes **normal Slimefun core, third-party addon and custom categories**.
Normal categories and their items can be rearranged. Special `FlexItemGroup`
categories are visible, but their bespoke inventories do not expose a standard
item list and cannot be reorganized at the item level.

## Category and item order

- Open a category by left- or right-clicking its icon.
- Core Slimefun categories are now included, identified by namespaced keys.
- Click a nested category to browse subgroups; these are not ordinary items.
- Use **Item List** in the main editor or `/sc item list` for saved templates.
- Shift-left a category to move it up; shift-right to move it down.
- Within a category, shift-left/right an item to reorder it.
- Open its crafting grid and select **Move to another category** to move it.
  Parent/nested groups cannot contain items.
- Order is stored in `editor-order.yml`; changing it may move custom
  categories to a dedicated Slimefun tier range. It does not rename IDs.

A category/item may appear with a generic icon in **recovery mode** if it
exists in YAML but did not finish Slimefun registration.

## Crafting grid and result

Choose the crafting machine at the **Crafting machine** button in the
recipe editor. Left-click advances, right-click goes backwards through
Enhanced Crafting Table, Magic Workbench, Armor Forge, Pressure Chamber,
Compressor, Smeltery, Ore Crusher, Grind Stone, and None.

The 3×3 layout contains **ghost previews**, not real inventory items:

1. Put an ingredient in your cursor and left-click a slot: copies its metadata
   and amount into the draft.
2. Left-click an empty cursor over a slot: clears it.
3. Right-click a nonempty slot: increases the required quantity by one.
   Shift-right decreases it. Amount cannot exceed the item's stack limit.
4. Edit the **Result** slot similarly, then select **SAVE recipe**.
5. Restart the server to activate the changed recipe. In recovery mode,
   a save updates YAML but does not fix every unrelated registration error.

### Ingredients and custom items

The editor recognizes Slimefun IDs first. Other ItemStacks with custom
metadata can be stored as `SAVEDITEM` files in `saveditems/`, preserving
their ItemMeta/PDC/model data. Plain vanilla items are stored by Material ID.
Do not remove generated saved-item files that are still referenced in recipes.

Legacy ingredient types: `NONE`, `VANILLA`, `SLIMEFUN`, `SAVEDITEM`.
Every ingredient has an `amount` field.

### Multi-quantity slots and Valhalla grid recipes

Native Slimefun enhanced crafting, magic workbench, armor forge and pressure
chamber recipes do not support multiple input items in a single grid slot.

For items in `items.yml`, enable **Valhalla grid mode** to register a
`VALHALLA_GRID` recipe through ValhallaMMO-Rewritten. This uses its
`DynamicGridRecipe`, `SlotEntry` and `ExactChoice` APIs, which enforce
required per-slot amounts and exact custom metadata. Slimefun's native
recipe type is set to `NONE` to avoid double crafting.

The optional fixed output multiplier (x1–x16) multiplies the result count
only and must not exceed a legal item stack. It does not add Valhalla skill
perk effects or invoke a second recipe processing transaction.

**Both dependencies must be compatible** to use this mode. Test normal
click/shift-click, insufficient amounts, custom items and output counts on
the test server before release.

## Item List and item saving

Open `/sc item list` to browse saved item templates. Left-click gives one
admin copy of the selected template. Click **Save item in hand** or run
`/sc item saveitem hand` to save the held item to a new numbered
`saveditems/<id>.yml` file. Its complete ItemStack metadata is preserved and
the hand item is not consumed. The old `/sc saveitem` and
`/sc getsaveditem gui` commands also open the same workflows.

## Machine processing editor

Right-click a registered machine and select a process, or choose **Add new
processing recipe**. Each process has two inputs, two outputs and duration
in seconds. Hold an item in the cursor to set a slot. The GUI saves to the
existing `machines.yml` format.

Delete requires two clicks. Changes are not loaded until a restart.
Only SlimeCustomizer machines are editable.

## Safe testing

Back up the data folder. The GUI normally does not take real items from the
player. Confirm items stay on the cursor, no additional items are created,
no output duplicates occur, and machine inventories survive a restart.

If registration fails, `/sc status` reports **RECOVERY MODE**. Use YAML
repair and restart; the GUI does not mark broken recipes valid automatically.
