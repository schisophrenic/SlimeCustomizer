# YAML configuration reference

Existing installations keep their original filenames and IDs. Changes to
recipes require a **server restart**, not `/reload`. Always back up
`plugins/SlimeCustomizer/` before editing.

## Categories

`categories.yml` stores groups displayed in the Slimefun Guide:

```yaml
slime_customizer:
  type: normal
  category-name: "&cSlimeCustomizer"
  category-item: REDSTONE_LAMP
  tier: 3
```

Supported `type` values: `normal`, `nested` (parent container),
`sub` (with `parent` pointing to a nested group), `locked` (with
`parents` list), and `seasonal` (with `month` 1–12).
Changing category order in the GUI writes `editor-order.yml`, not new IDs.

## Items

`items.yml` contains IDs, category, item material/model, item name/lore,
crafting recipe and result. Simplified excerpt:

```yaml
CUSTOM_ROD:
  category: slime_customizer
  item-type: CUSTOM
  item-id: STICK
  item-name: "&bCustom Rod"
  item-amount: 1
  item-lore:
    - "&7Custom item"
  placeable: false
  crafting-recipe-type: ENHANCED_CRAFTING_TABLE
  crafting-recipe:
    1: {type: VANILLA, id: STICK, amount: 1}
    2: {type: NONE, id: N/A, amount: 1}
```

The actual 3×3 grid consists of positions **1 through 9**. Keep all nine
positions in a complete recipe. Available ingredients are `VANILLA`,
`SLIMEFUN`, `SAVEDITEM`, `NONE`. `SAVEDITEM` references a
`saveditems/<id>.yml` template. A Slimefun item is referenced by its
registered ID, not its material.

Supported original recipe types include `ENHANCED_CRAFTING_TABLE`,
`MAGIC_WORKBENCH`, `ARMOR_FORGE`, `PRESSURE_CHAMBER`,
`COMPRESSOR`, `SMELTERY`, `ORE_CRUSHER`, `GRIND_STONE`, and
`NONE`. The GUI adds `VALHALLA_GRID` as an optional delegate for
normal custom items if ValhallaMMO-Rewritten is present.

GUI output editing adds an optional serialized `recipe-output` ItemStack
to an entry. That changes the *crafting result* without changing the
Slimefun item's registered identifier. A fixed Valhalla multiplier is stored
as `valhalla-output-multiplier`.

## Machines

`machines.yml` defines `block-type`, `progress-bar-item`,
`stats.energy-consumption`, `stats.energy-buffer`, a 3×3 crafting
recipe for the machine block itself, and a `recipes` section for
processing operations.

```yaml
  recipes:
    1:
      speed-in-seconds: 5
      input:
        1: {type: VANILLA, id: IRON_INGOT, amount: 9}
        2: {type: NONE, id: N/A, amount: 1}
      output:
        1: {type: VANILLA, id: IRON_BLOCK, amount: 1}
        2: {type: NONE, id: N/A, amount: 1}
```

The legacy machine processor supports **up to two inputs and two outputs**.
The editor can add, edit or delete a processing entry. Use amounts no greater
than the corresponding item's max stack size.

## Generators

`generators.yml` defines fuel inputs and optional returned items under
`recipes.<id>.input` and `recipes.<id>.output`, with
`time-in-seconds`. It also specifies `stats.energy-production`,
`stats.energy-buffer`, `block-type` and the generator's own crafting
recipe. The GUI currently edits **the generator's crafting recipe**, not
the full fuel-processing list.

`solar-generators.yml` specifies day/night production under
`stats.energy-production.day` and `.night`. Its crafting recipe can be
edited with the GUI.

## Mob drops

`mob-drops.yml` defines item ID, `mob`, `chance` (0–100), output
item name/lore/material or saved item, and a `recipe-display-item` for the
guide. This is a drop configuration, not a 3×3 craftable recipe. The editor
does not currently provide a full mob drop editor.

## References to other categories

The legacy `existing:namespace:key` syntax can display custom entries
inside existing Slimefun groups. The GUI's move operation intentionally
only allows categories owned by SlimeCustomizer.

## Automatic migrations

The plugin has legacy format migration helpers. Review source changes and
back up YAML before testing newly updated versions. Failed registration is
reported by `/sc status`; fix offending YAML and restart.
