## Slimefun-Legacy modernization (development branch)

This branch is a **compatibility candidate**, not a tested production release.
It preserves the `SlimeCustomizer` Bukkit plugin name, existing Slimefun item IDs,
`saveditems/`, and all existing YAML configuration filenames.

**Targets:** Paper/Purpur Minecraft 1.21.11 (Java 21+) and 26.1–26.3
(Java 25+). The plugin requires the **Slimefun-Legacy** fork, not a
second/duplicate Slimefun core. The tested dependency baseline is the published
4.1.80 JAR. Its built-in compatibility manifest is intentionally marked
*untested* until real-server smoke tests pass.

### Building

On a machine with Maven, `curl`, and **JDK 25**:

```bash
bash scripts/install-slimefun-legacy.sh
mvn -B clean verify
```

The installer downloads the official pinned Slimefun-Legacy release JAR and
installs it to your **local Maven cache** as a compile-only dependency.
This does not bundle the Slimefun core into the final plugin.

Use `mvn -B -Dpaper.api.version=26.3.build.166-beta clean verify`
to check the Paper 26.3 API instead of the default 1.21.11 API.
The CI workflow tests both configurations and uploads test artifacts if
compilation succeeds. Building is **not** proof of in-game compatibility.

### Upgrade testing

Back up your server and `plugins/SlimeCustomizer` data folder before testing.
Run Slimefun-Legacy 4.1.80 as the **only** Slimefun core JAR. Test startup,
Slimefun Guide registration, custom items, saved items, custom recipes, machine
processing, generators, solar power, and mob drops on both server versions.
Also check that placed machine block data and inventory contents survive a restart.

The retired upstream BlobBuildUpdater does not run in this branch. Future
updates should come from reviewed releases instead of automatic replacements.
