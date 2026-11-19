# Startup troubleshooting and recovery mode

## `Cannot execute command ... plugin is disabled`

This error is **secondary**, not the root cause. Find the earlier
`[SlimeCustomizer]`, `[SC-STARTUP]` and `[Slimefun]` lines during startup.
Common reasons include an absent/incompatible Slimefun core, missing
required Java version, an unreadable data folder or malformed item/recipe
configuration.

### Case A: compatibility or file initialization failure

If Slimefun-Legacy is disabled/missing, the platform/Java version fails
preflight, or the data directory cannot be created, the plugin correctly
disables. `/sc` commands cannot run because Bukkit disables plugin commands.

Check the first startup **SEVERE/ERROR** entry and fix the dependency,
platform or file permission problem before trying again.

### Case B: custom recipe, item or category registration failure

The updated development build logs the stage it was registering:

```text
[SC-STARTUP] Loading categories.yml
[SC-STARTUP] Loading items.yml
[SC-STARTUP] Registration failed during items.yml: ...
[SC-STARTUP] Admin EDITOR RECOVERY MODE is active
```

It leaves **only the admin editor and status commands usable** for
config repairs. `/sc status` reports the failing stage and reason.
`/sc editor` can browse YAML-defined categories/items, even if they
were never successfully registered with Slimefun.

Correct the configuration, stop the server, back it up and restart.
**Do not run production gameplay in recovery mode**. Partial Slimefun
registrations may have been created earlier in the startup pass.

## `No categories were found`

Check that `plugins/SlimeCustomizer/categories.yml` exists and has at least
one valid YAML category section. The clean-install fix creates the plugin
folder before copying all six default YAML resources. An existing config
will **never be overwritten** by the installer.

## What to provide with a bug report

- The **first** SlimeCustomizer error at server startup and the following
  stack trace, **not only** the error when the command was used.
- The build/commit name, Purpur or Paper full build, Java version and
  Slimefun-Legacy version.
- `/sc status` output if available.
- Relevant config section with sensitive keys/paths removed.
- Steps to reproduce with a backed-up test world.

## Clicking an addon category throws `A FlexItemGroup has no items!`

Slimefun's FlexItemGroup and nested containers are custom category
interfaces, not normal inventories. Earlier GUI tests called `getItems()`
on them, which throws. The category organizer now detects these classes:
it shows a category row without requesting a nonexistent item list and lets
you browse nested child groups where possible. Use the owning addon's UI for
its custom contents. The ordinary Slimefun categories and their items remain
reorderable.
