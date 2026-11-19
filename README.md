# SlimeCustomizer, Slimefun-Legacy Edition

A development fork of the original **SlimeCustomizer**, updated for the maintained
[Slimefun-Legacy](https://github.com/wickidcow/Slimefun-Legacy) core and
Paper/Purpur 1.21.11 and 26.3. It lets server administrators define custom
Slimefun categories, items, machines, generators and mob drops in YAML, with
an **in-game GUI editor** for item arrangement and crafting recipes.

> **TEST BUILD.** Java/API compilation and automated configuration tests do
> not prove runtime compatibility. Use a backed-up test server. Do not merge
> into production until in-game results have been verified.

## Installation

1. Back up the server, player data and `plugins/SlimeCustomizer/`.
2. Install **Slimefun-Legacy** as the only Slimefun core. The build baseline
   is version **4.1.80**. Do not install the old Slimefun4 core at the same time.
3. Use Java 21+ for Paper 1.21.11 or Java 25+ for Paper/Purpur 26.3.
4. Download the matching **runnable JAR** from
   [GitHub Actions](https://github.com/schisophrenic/SlimeCustomizer/actions)
   for the `feature/gui-recipe-editor-valhalla-bridge` branch and place it in
   `plugins/`. Remove older duplicate SlimeCustomizer JARs.
5. Restart the server. The `plugins/SlimeCustomizer/` directory and default
   YAML files will be created on first launch. Existing config and
   `saveditems/` files are preserved.
6. Run **`/sc status`**. If it reports `READY`, try **`/sc editor`**.

ValhallaMMO-Rewritten is **optional**. Native Slimefun recipes and the GUI
work without it. Recipes explicitly configured as `VALHALLA_GRID` require
its compatible grid crafting API and will not craft without it.

## Commands

The **Item List** is reachable from the main editor or with
`/sc item list`. Run `/sc item saveitem hand` to save the held item,
including custom metadata, without consuming it. `/sc saveitem` and
`/sc getsaveditem gui` remain as compatibility commands.

| Command | Function |
| --- | --- |
| `/sc item list` | Browse and obtain saved item templates |
| `/sc item saveitem hand` | Save held item as a template without consuming it |
| `/sc status` | Show startup registration status and the failing configuration stage |
| `/sc editor` | Open category, item and recipe GUI |
| `/sc saveitem` | Save the item in hand, including metadata |
| `/sc give <player> <slimefun_id> [amount]` | Give a registered item |
| `/sc getsaveditem gui` | Browse saved item templates |
| `/sc getsaveditem <id> <player> <amount>` | Give a saved item |
| `/sc categories` | Inspect Slimefun category namespace identifiers |

Editor and modification commands require `slimecustomizer.admin` (default: OP).
`/sc status` is intended for admins diagnosing startup.

## Admin editor

The organizer includes normal Slimefun core, addon and SlimeCustomizer
categories. FlexItemGroups have custom inventory contents that cannot be
reordered as a flat list. Nested category containers expose subgroups instead.
Shift-left/right to move a category or item up/down. Left-click an item
for its 3×3 crafting grid; right-click a machine for its processing recipes.
Select the desired crafting station using the **Crafting machine** selector
(Enhanced Crafting Table, Magic Workbench, Armor Forge, Pressure Chamber,
Compressor, Smeltery, Ore Crusher, Grind Stone or None).
Use an item on the cursor and left-click a grid slot to copy its identity
and stack amount without consuming the item. Save changes to YAML and
restart the server to apply recipe changes. Guide ordering updates live.

A compatible ValhallaMMO-Rewritten instance can process special 3×3
recipes with exact metadata and **multiple units per slot**. Standard
Slimefun enhanced crafting recipes continue to use their normal limits.
The fixed Valhalla result multiplier is currently **x1–x16**. It does not
modify Valhalla skill/perk multiplier logic.

**Implemented:** reorder and move items, edit one crafting recipe result
and 3×3 inputs per item, add/edit/delete two-input/two-output machine
processing recipes, optional Valhalla grid bridge.

**Not implemented yet:** multiple alternative 3×3 crafting recipes per item,
live hot-reloading of processing recipes, full Valhalla perk multiplier
interoperability, or general-purpose editing of other add-ons' registrations.

See the [GUI guide](docs/GUI_EDITOR.md) for controls and restrictions.

## Configuration and compatibility

The old YAML filenames and custom Slimefun item IDs are retained:
`categories.yml`, `items.yml`, `machines.yml`, `generators.yml`,
`solar-generators.yml`, `mob-drops.yml` and `saveditems/`.
The GUI additionally writes `editor-order.yml` and optional saved-item
templates for exact item metadata.

- [YAML configuration reference](docs/CONFIGURATION.md)
- [GUI editor instructions](docs/GUI_EDITOR.md)
- [Compatibility and test checklist](docs/COMPATIBILITY.md)
- [Startup errors and recovery mode](docs/TROUBLESHOOTING.md)

## Startup errors and recovery mode

A failing `/sc editor` command with **"plugin is disabled"** does not
identify the cause. Review the *earlier* startup log lines containing
`[SC-STARTUP]`, `[SlimeCustomizer]` or `[Slimefun]`.

This development build registers the admin editor **before** loading custom
items. If a YAML recipe is rejected, the editor remains available in
**RECOVERY MODE** and `/sc status` reports which file failed. Gameplay
registration may then be incomplete: correct YAML and **restart**.
If the mandatory Slimefun-Legacy dependency/platform preflight fails, Bukkit
still disables the plugin and the command cannot run. Refer to
[troubleshooting](docs/TROUBLESHOOTING.md) for the difference.

## Building from source

Requires JDK 25, Maven, Bash and curl:

```bash
bash scripts/install-slimefun-legacy.sh
mvn -B clean verify
```

The installer pins the public Slimefun-Legacy 4.1.80 JAR in the local Maven
repository; its classes are **not** shaded into this plugin. CI checks
Paper API 1.21.11 and 26.3 and uploads test artifacts.

## Credits and license

This fork continues work by **NCBPFluffyBear** and contributors. The original
code is GPL-licensed; see [LICENSE.txt](LICENSE.txt). The current fork and
development issue tracker are at
[schisophrenic/SlimeCustomizer](https://github.com/schisophrenic/SlimeCustomizer).
