# Compatibility and testing

## Supported build targets

| Component | Tested status |
| --- | --- |
| Paper API 1.21.11 | CI compilation and configuration tests |
| Paper API 26.3 | CI compilation and configuration tests |
| Purpur 26.3 | Runtime validation in progress |
| Java | Java 21 bytecode; Java 25 runtime for Minecraft 26.3 |
| Slimefun core | Slimefun-Legacy 4.1.80 build dependency, required at runtime |
| ValhallaMMO-Rewritten | Optional bridge, runtime verification in progress |

Do not install the legacy/original Slimefun4 core alongside Slimefun-Legacy.
`SlimeCustomizer` keeps its original Bukkit plugin name and item ID format.

## Startup and persistence checks

1. On a **fresh** test server, check that all six YAML files and the
   `saveditems` folder are created.
2. Run `/sc status` and confirm **READY**. For **RECOVERY MODE**, examine
   the earliest `[SC-STARTUP]` error.
3. Open `/sc editor`. Browse categories and items, then reorder both.
4. Save a changed crafting result and a new machine processing recipe;
   restart and verify the new result and processing behavior.
5. Save a custom item with metadata; ensure that recognition and stacks
   remain correct after restart.
6. Test missing ingredients, exact Slimefun/Valhalla items, four units in
   one slot (Valhalla grid), insufficient quantities, shift-click and
   output multiplier. Verify no duplication or free crafting.
7. Test generators, solar production, mob drops, placed machines, inventories
   and saved item migration.
8. Test with ValhallaMMO absent. Basic Slimefun functionality must continue;
   `VALHALLA_GRID` recipes should remain unavailable and report why.

These items have **not all passed in-game yet**. A successful compile alone
is not a pass for crafting transactions, scheduling or world persistence.

## Source build

```bash
bash scripts/install-slimefun-legacy.sh
mvn -B clean verify
```

Run using JDK 25. GitHub Actions builds with two Paper API targets and runs
regression tests before packaging the shaded JAR. It does not package
Slimefun or ValhallaMMO inside the plugin.

## Known limitations

- No live hot-reload of recipe changes.
- The GUI currently manages only one 3×3 recipe per custom item.
- Native Slimefun multiblock crafting types cannot consume multiple items
  from one slot; the Valhalla grid delegate is the special path.
- The x1–x16 Valhalla output setting is a fixed recipe multiplier, not
  automatic skill perk integration.
- The Valhalla bridge's class/API compatibility needs server verification.
- An invalid recipe can put the plugin into admin recovery mode. Partial
  registrations should not be treated as production-safe.
