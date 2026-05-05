# CLAUDE.md

File guide Claude Code (claude.ai/code) for work in this repo.

## Build & run

Kotlin/Gradle multi-project. Java 25 toolchain, Kotlin 2.3, Gradle wrapper (`./gradlew`). CI use JDK 21 to call wrapper, but toolchain in `buildSrc/.../kotlin-script.gradle.kts` need JDK 25 to compile.

- `./gradlew build` — build all (jars go `<module>/build/libs/`).
- `./gradlew :veinminer:paper:build` / `:veinminer:fabric:build` / `:veinminer:neoforge:build` — build one base-mod loader.
- `./gradlew :veinminer-client:fabric:build` / `:veinminer-client:neoforge:build` — build the client addon for one loader.
- `./gradlew :veinminer-enchant:build` — build the enchantment fatjar.
- `./gradlew :veinminer:paper:runServer` — start Paper dev server (via `xyz.jpenilla.run-paper`); `runFolia` also registered.
- `./gradlew :veinminer:fabric:runServer` / `:veinminer-client:fabric:runClient` — start via fabric-loom (each got `run/` dirs).
- `./gradlew :datapacks:zipAll` — zip V1 datapacks (`zipVeinminer`, `zipEnchantment` for single ones).
- `./gradlew :veinminer:paper:modrinth` (or any module's `modrinth` task, via `com.modrinth.minotaur`) — publish. Set `modrinthToken` in `gradle.properties` or via `-PmodrinthToken=...`.

No test source set anywhere — don't make up test commands.

Game/loader versions live in `gradle.properties` (`gameVersion`, `paperVersion`, `fabricSupportedVersions`, `fabricLoaderVersion`, `neoforgeVersion`, `fancyModLoaderVersion`, `silkVersion`, `adventureVersion`). Bump Minecraft = edit those, not per-module build files.

## Architecture

Gradle layout (see `settings.gradle.kts`):

```
core                     ← shared logic, settings model, packet IDs, update checker (applies fabric-loom for MC types)
veinminer/
 ├── common              ← base-mod common code (applies fabric-loom; depends on :core)
 ├── paper               ← Paper/Folia plugin (paperweight; depends on :core + :veinminer:common)
 ├── fabric              ← Fabric server+client mod (loom; `include`s :veinminer:common which chains :core)
 └── neoforge            ← NeoForge base-mod (stub, in progress)
veinminer-client/
 ├── common              ← client-addon common code (loom)
 ├── fabric              ← Fabric client-only addon: hotkey + block highlight (depends on :veinminer:fabric)
 └── neoforge            ← NeoForge client-addon (stub, in progress)
veinminer-enchant        ← Separate plugin/mod registering the Veinminer enchantment (fatjar across loaders)
datapacks                ← Zips the legacy V1 datapacks shipped to Modrinth
```

`core` and `veinminer/common` are loader-agnostic Kotlin but **do** see Minecraft types (both apply `net.fabricmc.fabric-loom` and pull `minecraft("com.mojang:minecraft:$gameVersion")`) so abstractions over blocks/items/world live there. They still avoid Bukkit/Fabric-API specifics — those leak only into the loader-leaf modules.

`core` owns:
- `config.data.VeinminerSettings` / `VeinminerClientSettings` / `VeinminerSettingsOverride` — canonical settings model (kotlinx-serialization). `applyOverrides(isClient, group)` enforce precedence **default < client-installed < block-group override**. Any new tunable must thread through all three classes plus `applyOverrides`.
- `config.data.BlockGroup` — generic over block/item type `T`; loader modules construct with native types (`NamespacedKey` for Paper, `Identifier` for Fabric) and provide a custom serializer.
- `config.network.NetworkManager` + `C2SPackets` / `S2CPackets` — channel name `veinminer` and packet IDs (`join`, `mine`, `key`, `configuration`, `highlight`). All loader networking implementations register against these constants; client and server speak the same wire protocol.
- `config.UpdateManager` — Modrinth update check used by every loader's `Veinminer` entrypoint.

`veinminer/common` holds base-mod code shared across Paper/Fabric/NeoForge (e.g. shared network payload definitions under `network/`). Loader leaf modules each carry the same shape: `Veinminer.kt` (entry point), `VeinMinerEvent.kt` (mining event handle — break spread, drops, durability), `command/VeinminerCommand.kt`, `config/ConfigManager.kt` + `ConfigSerializer.kt` (serializers for loader-native types). Paper and Fabric `ConfigManager`s are sibling implementations, **not** a shared interface — keep features in sync by editing both (and the NeoForge one once it lands).

`commons/default_groups.json` is wired into Paper and Fabric jars via `sourceSets { main { resources.srcDirs("$rootDir/commons/") } }`. Edit once.

`buildSrc/src/main/kotlin/*.gradle.kts` = convention plugins applied via `` `kotlin-script` ``, `` `fabric-script` ``, `` `paper-script` ``, etc. Cross-cutting build changes (Kotlin version, JVM target, mod metadata `processResources` substitution for `fabric.mod.json` / `neoforge.mods.toml`) live there, not in module `build.gradle.kts`.

### Multi-loom dependency rule

Because `:core` and `:veinminer:common` both apply fabric-loom, the loader leaf modules must take **one** path to `:core` to avoid a circular `remapJar` graph. Convention: each leaf consumes only its direct parent (e.g. `:veinminer:fabric` includes `:veinminer:common`; `:veinminer-client:fabric` depends on `:veinminer:fabric`); the chain carries `:core` transitively. Don't add a second direct `include(project(":core"))` from a leaf that already pulls it through `:veinminer:common` / `:veinminer:fabric`.

## Conventions worth knowing

- Permissions: defined as `const val` in `core/.../config/utils/Globals.kt` (`veinminer.toggle`, `veinminer.blocks`, `veinminer.settings`, `veinminer.use`, `veinminer.groups`, `veinminer.reload`). OP/cheats bypass them. Use these constants — don't inline strings.
- `usePermissions`, `useConfig`, `useBrigadier`, `foliaSupport` in `gradle.properties` are real toggles read by the convention plugins; flipping them changes the dependency graph.
- Fabric uses access-wideners (`veinminer/fabric/src/main/resources/veinminer.accesswidener`, plus a separate one for the client addon under `veinminer-client/fabric`). Touching private MC fields means editing those files, not adding mixins.
- Paper module uses `paperweight` with `MOJANG_PRODUCTION` reobf; the Brigadier integration is via CommandAPI shaded into the plugin (`dev.jorel:commandapi-paper-shade`), not the Paper Brigadier API directly.
- Paper module's `paper { main = ... }` and the enchant module's `paper { main = ... }` use the `de.eldoria.plugin-yml.paper` plugin to generate `paper-plugin.yml` at build time — there is no checked-in `plugin.yml`.
- All loader implementations of the network layer share the same packet IDs declared in `core`'s `NetworkManager`; if you change a packet's payload, update the matching networking code in `:veinminer:fabric`, `:veinminer:paper`, `:veinminer:neoforge`, **and** the relevant client-addon module.

## Two product lines, don't confuse them

- **V2** (this codebase's main output) — the Fabric/NeoForge mods and Paper plugin.
- **V1** — the lightweight datapacks under `datapacks/veinminer/` and `datapacks/enchantment/`, configured purely via `/function veinminer:_config`. They have their own versioning (see `datapacks/build.gradle.kts`) and ship separately to Modrinth. The two are independent — a V2 fix usually does not apply to V1.

## Ongoing rewrite

NeoForge support is mid-landing. Tracker:
- [x] New module layout (`veinminer/{common,fabric,paper,neoforge}`, `veinminer-client/{common,fabric,neoforge}`)
- [x] Silk dependency removed from base mod
- [ ] NeoForge build pipeline filled in (`:veinminer:neoforge`, `:veinminer-client:neoforge` are still stubs)
- [ ] `:veinminer-enchant` repacked as a multi-loader fatjar