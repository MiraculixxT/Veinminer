# CLAUDE.md

File guide Claude Code (claude.ai/code) for work in this repo.

## Build & run

Kotlin/Gradle multi-project. Java 25 toolchain, Kotlin 2.3, Gradle wrapper (`./gradlew`). CI use JDK 21 to call wrapper, but toolchain in `buildSrc/.../kotlin-script.gradle.kts` need JDK 25 to compile.

- `./gradlew build` — build all (jars go `<module>/build/libs/`).
- `./gradlew :veinminer-paper:build` / `:veinminer-fabric:build` / `:veinminer-client:build` / `:veinminer-enchant:build` — build one loader.
- `./gradlew :veinminer-paper:runServer` — start Paper dev server (via `xyz.jpenilla.run-paper`); `runFolia` also registered.
- `./gradlew :veinminer-fabric:runServer` / `runClient` — start via fabric-loom (Fabric and client mod both got `run/` dirs).
- `./gradlew :datapacks:zipAll` — zip V1 datapacks (`zipVeinminer`, `zipEnchantment` for single ones).
- `./gradlew :veinminer-paper:modrinth` (or any module's `modrinth` task, via `com.modrinth.minotaur`) — publish. Set `modrinthToken` in `gradle.properties` or via `-PmodrinthToken=...`.

No test source set anywhere — don't make up test commands.

Game/loader versions live in `gradle.properties` (`gameVersion`, `paperVersion`, `fabricSupportedVersions`, `silkVersion`, `adventureVersion`). Bump Minecraft = edit those, not per-module build files.

## Architecture

Five Gradle modules under one root, plus sixth (`datapacks`) that only zip legacy V1 datapacks shipped to Modrinth.

```
core             ← shared logic, configuration model, packet IDs, update checker
 ├── veinminer-paper    (Paper/Folia plugin, depends on core via `implementation project(":core")`)
 ├── veinminer-fabric   (Fabric server+client mod, `include`s core into the fat jar)
 ├── veinminer-client   (Fabric client-only addon: hotkey + block highlight; depends on -fabric)
 └── veinminer-enchant  (Separate plugin/mod that registers the Veinminer enchantment)
```

`core` loader-agnostic Kotlin — no Bukkit, no Minecraft classes leak in. Owns:
- `config.data.VeinminerSettings` / `VeinminerClientSettings` / `VeinminerSettingsOverride` — canonical settings model (kotlinx-serialization). `applyOverrides(isClient, group)` enforce precedence **default < client-installed < block-group override**. Any new tunable must thread through all three classes plus `applyOverrides`.
- `config.data.BlockGroup` — generic over block/item type `T`; loader modules make with native types (`NamespacedKey` for Paper, `Identifier` for Fabric) and give custom serializer.
- `config.network.NetworkManager` + `C2SPackets` / `S2CPackets` — channel name `veinminer` and packet IDs (`join`, `mine`, `key`, `configuration`, `highlight`). Both loaders' `networking/Packets.kt` register against these constants; client and server speak same wire protocol.
- `config.UpdateManager` — Modrinth update check used by both loaders' `Veinminer` entrypoints.

Loader modules each do same shape: `Veinminer.kt` (entry point), `VeinMinerEvent.kt` (actual mining event handle — break spread, drops, durability), `command/VeinminerCommand.kt`, `config/ConfigManager.kt` + `ConfigSerializer.kt` (serializers for loader-native types so `core` generic model load). Paper and Fabric `ConfigManager`s are sibling implementations, **not** shared interface — keep features in sync by edit both.

`commons/default_groups.json` wired into both Paper and Fabric jars via `sourceSets { main { resources.srcDirs("$rootDir/commons/") } }`. Edit once.

`buildSrc/src/main/kotlin/*.gradle.kts` = convention plugins applied via `` `kotlin-script` ``, `` `fabric-script` ``, `` `paper-script` ``, etc. Cross-cutting build changes (Kotlin version, JVM target, mod metadata `processResources` substitution for `fabric.mod.json`) live there, not in module `build.gradle.kts`.

## Conventions worth knowing

- Permissions: defined as `const val` in `core/.../config/utils/Globals.kt` (`veinminer.toggle`, `veinminer.blocks`, `veinminer.settings`, `veinminer.use`, `veinminer.groups`, `veinminer.reload`). OP/cheats bypass them. Use these constants — don't inline strings.
- `usePermissions`, `useConfig`, `useBrigadier`, `foliaSupport` in `gradle.properties` are real toggles read by the convention plugins; flipping them changes the dependency graph.
- Fabric uses access-wideners (`veinminer-fabric/src/main/resources/veinminer.accesswidener`, plus a separate one for the client module). Touching private MC fields means editing those files, not adding mixins.
- Paper module uses `paperweight` with `MOJANG_PRODUCTION` reobf; the Brigadier integration is via CommandAPI shaded into the plugin (`dev.jorel:commandapi-paper-shade`), not the Paper Brigadier API directly.
- Paper module's `paper { main = ... }` and the enchant module's `paper { main = ... }` use the `de.eldoria.plugin-yml.paper` plugin to generate `paper-plugin.yml` at build time — there is no checked-in `plugin.yml`.
- The Fabric client and the Paper/Fabric servers share the same packet IDs in `NetworkManager`; if you change a packet's payload, update the matching `Packets.kt` in `veinminer-fabric`, `veinminer-paper`, **and** `veinminer-client`.

## Two product lines, don't confuse them

- **V2** (this codebase's main output) — the Fabric mod and Paper plugin.
- **V1** — the lightweight datapacks under `datapacks/veinminer/` and `datapacks/enchantment/`, configured purely via `/function veinminer:_config`. They have their own versioning (see `datapacks/build.gradle.kts`) and ship separate to Modrinth. Two independent — fix bug in V2 usually no apply to V1.