# Overview

## Build & Run

Kotlin/Gradle multi-project. Java 25 toolchain, Kotlin 2.3, Gradle wrapper (`./gradlew`).

- `./gradlew build` - build all modules.
- `./gradlew :veinminer:veinminer-paper:build` / `:veinminer:veinminer-fabric:build` / `:veinminer:veinminer-neoforge:build` - build one base-mod loader.
- `./gradlew :veinminer-client:veinminer-client-fabric:build` / `:veinminer-client:veinminer-client-neoforge:build` - build one client addon loader.
- `./gradlew :veinminer-enchant:build` - build the enchantment addon jar for Paper/Folia, Fabric/Quilt, and NeoForge.
- `./gradlew :veinminer:veinminer-paper:runServer` - start a Paper dev server via `xyz.jpenilla.run-paper`; `runFolia` is also registered.
- `./gradlew :veinminer:veinminer-fabric:runServer` / `:veinminer-client:veinminer-client-fabric:runClient` - start Fabric dev runs via loom.
- `./gradlew :veinminer:veinminer-neoforge:runServer` / `:veinminer-client:veinminer-client-neoforge:runClient` - start NeoForge dev runs via moddev.
- `./gradlew :datapacks:zipAll` - zip the V1 datapacks (`zipVeinminer`, `zipEnchantment` for single packs).
- `./gradlew :veinminer:veinminer-paper:modrinth` and the other module `modrinth` tasks publish through Minotaur. Set `modrinthToken` in `gradle.properties` or pass `-PmodrinthToken=...`.
- `./gradlew :veinminer:veinminer-fabric:curseforge :veinminer:veinminer-neoforge:curseforge` and the matching client/enchantment tasks publish Fabric/NeoForge artifacts to CurseForge. Set `curseforgeToken` plus the relevant numeric `curseforgeId`, `curseforgeClientId`, and `curseforgeEnchantmentId`; blank IDs skip that module. Set `curseforgeSlug` / `curseforgeClientSlug` when CurseForge dependency relations should point between the base and client projects.

The doubled project path segments (`:veinminer:veinminer-fabric`, `:veinminer-client:veinminer-client-common`, etc.) are intentional. `settings.gradle.kts` gives leaf projects unique names so Gradle does not confuse `:veinminer:fabric` with `:veinminer-client:fabric`, or the two `common`/`neoforge` modules.

No test source set exists at the moment, so do not document or rely on test tasks that are not present.

Version and publishing knobs live in `gradle.properties`: `version`, `gameVersion`, `paperVersion`, `fabricSupportedVersions`, `neoforgeVersion`, `paperSupportedVersions`, `neoforgeSupportedVersions`, `enchantmentVersions`, `adventureVersion`, and the Modrinth/CurseForge changelog/project IDs. Bump Minecraft support there rather than in individual module build files.

## Architecture

Gradle layout:

```text
core                     <- shared settings, block groups, command DSL/permissions, packets, pattern engine, update checker
veinminer/
 ├── common              <- base-mod shared code for Fabric/NeoForge plus shared command/config/network glue
 ├── paper               <- Paper/Purpur/Folia plugin
 ├── fabric              <- Fabric/Quilt base mod
 └── neoforge            <- NeoForge base mod
veinminer-client/
 ├── common              <- client addon shared keybind, preview selection, HUD, highlight, and network state
 ├── fabric              <- Fabric/Quilt client addon hooks, keybindings, mixins, render adapters
 └── neoforge            <- NeoForge client addon hooks, keybindings, render adapters
veinminer-enchant        <- separate addon registering the Veinmine enchantment across Paper/Fabric/NeoForge
datapacks                <- zips the legacy V1 datapacks shipped separately
```

`core` and the two `common` projects are still Minecraft-aware: they apply fabric-loom or compile against Minecraft types where needed. They should stay free of Bukkit/Paper, Fabric API, and NeoForge-specific APIs. Loader APIs belong in the leaf modules.

`core` owns:

- `data/VeinminerSettings.kt` - canonical settings model. Override precedence is `default < client-installed < block-group override`. New tunables usually need `VeinminerSettings`, `VeinminerClientSettings` or `VeinminerSettingsOverride`, command wiring, serialization compatibility, and client/network consideration.
- `data/BlockGroup.kt` - generic block/tool groups and fixed parsed groups.
- `pattern/*` - unified vein traversal: normal, tunnel 1x1/2x2/3x3, and flat shapes; `Surface`; shared target eligibility; `Veinmining.veinmine`.
- `network/*` - packet IDs and codecs. Current channels are `join` and `key` client-to-server, and `configuration` server-to-client.
- `command/*` and `utils/Globals.kt` - Brigadier DSL, permission adapter, permission constants, JSON defaults, and shared host interface.
- `UpdateManager.kt` - Modrinth update checking.

`veinminer/common` owns the base-mod shared runtime:

- `event/VeinMinerEvent.kt` - canonical NMS-based mining implementation used by Fabric and NeoForge, including cooldown, durability, drops, mining speed modifier, shape/depth handling, group matching, and enchantment checks.
- `event/EventState.kt` - loader-populated state used by the common event algorithm.
- `config/BaseConfigManager.kt`, `ConfigManager.kt`, and serializers - settings/groups/blocks loading, raw-to-native parsing, defaults, network cache refresh, and config broadcasts.
- `network/*` - server/client routers, loopback handling for integrated servers, platform callback interfaces, and payload adapters.
- `command/VeinminerCommand.kt` - shared `/veinminer` command tree: `reload`, `blocks`, `toggle`, `settings`, `groups`, and `presets`.

Paper is intentionally different: `veinminer/paper/src/main/kotlin/de/miraculixx/veinminer/VeinMinerEvent.kt` is the Bukkit/Paper implementation and does not use the NMS common event implementation. It still shares `core` data/packets/commands and much of `veinminer/common` config/network code.

`veinminer-client/common` is no longer a placeholder. It contains `KeyBindManager`, `ClientVeinSelector`, client network state, HUD rendering, block highlighting, and shape roulette state. Fabric and NeoForge client leaves provide loader-specific registration, keybinding declarations, network transport, and render hook adapters.

## Resources & Packaging

Shared resources are wired into leaf modules with `sourceSets { main { resources.srcDirs(...) } }`:

- `veinminer/assets/` - base mod icon and `default_groups.json`, consumed by Paper, Fabric, and NeoForge base-mod leaves.
- `veinminer-client/assets/` - client addon icon, lang files, HUD sprites, and shape sprites, consumed by Fabric and NeoForge client addon leaves.
- `veinminer-enchant/src/main/resources/` - enchantment addon metadata, icon/lang, enchantment JSON, and enchantment tags.

Fabric base mod embeds `:core` and `:veinminer:veinminer-common` with loom `include`. Fabric client addon depends on `:veinminer:veinminer-fabric`, compile-only references `:core` and base common, and includes `:veinminer-client:veinminer-client-common`.

NeoForge base mod depends on `:core` and `:veinminer:veinminer-common`, adds those source sets to the moddev `mods` block, and copies their compiled output into the jar. NeoForge client addon does the same for `:veinminer-client:veinminer-client-common` while depending on the NeoForge base mod at runtime.

Paper uses `paperweight` with `MOJANG_PRODUCTION` reobf and `shadowJar` as the published artifact. `de.eldoria.plugin-yml.paper` generates `paper-plugin.yml`; there is no checked-in plugin descriptor.

## Build Conventions

`buildSrc/src/main/kotlin/*.gradle.kts` contains the convention plugins:

- `kotlin-script` - Kotlin JVM, serialization, coroutines, reflect, Java 25 toolchain, `-Xcontext-parameters`.
- `fabric-script` - fabric-loom, Outlet version resolution, Fabric API, Fabric Language Kotlin, fabric-permissions-api, and resource token expansion.
- `neoforge-script` - moddev, NeoForge runs, KotlinLangForge, and `neoforge.mods.toml` token expansion.
- `paper-script` - paperweight, run-paper, generated plugin metadata, Kotlin libraries, and kpaper.
- `publish-script` - Modrinth Minotaur, CurseForgeGradle upload tasks, and Outlet release metadata.
- `shadow-script` - Paper dependency shadowing for project-local dependencies.

Make cross-cutting dependency, toolchain, metadata, and resource-token changes in these convention plugins unless a leaf module truly needs special behavior.

## Networking

The wire protocol is centralized in `core/src/main/kotlin/de/miraculixx/veinminer/network`:

- `JoinInformation` announces the client addon version.
- `KeyPress` carries hotkey state, selected shape, selected max depth, and target surface.
- `ServerConfiguration` syncs settings, groups, whitelisted blocks, enchantment state, host active state, and `veinminer.use` permission state to clients.

Loader leaves only implement `PlatformNetwork`/`ClientPlatformNetwork` and callbacks. Packet names and codecs should not be duplicated in loader code. If a packet payload changes, update the core data class/codecs and then verify all loader builds.

## Config & Commands

Runtime config files are `settings.json`, `blocks.json`, and `groups.json` under the platform config/plugin directory. `BaseConfigManager` loads `default_groups.json` from resources when no groups file exists. Raw strings are kept for persistence and reparsed into native identifiers (`Identifier` or `NamespacedKey`) for runtime use.

Permission constants live in `core/src/main/kotlin/de/miraculixx/veinminer/utils/Globals.kt`:

- `veinminer.toggle`
- `veinminer.blocks`
- `veinminer.settings`
- `veinminer.use`
- `veinminer.groups`
- `veinminer.reload`

Use the constants, not inline strings. Paper checks Bukkit permissions/op state, Fabric uses `me.lucko:fabric-permissions-api` plus vanilla gamemaster fallback, and NeoForge command permissions use gamemaster command level. NeoForge veinmining itself currently treats the runtime permission hook as allowed because NeoForge has no matching server permission API here.

Current settings include `cooldown`, `mustSneak`, `delay`, `maxChain`, `needCorrectTool`, `searchRadius`, `permissionRestricted`, `mergeItemDrops`, `autoUpdate`, `decreaseDurability`, `hungerPerBlock`, `miningSpeedModifier`, and `debug`. Client settings include `allow`, `require`, `translucentBlockHighlight`, `allBlocks`, and client overrides.

## Loader Notes

- Fabric uses access wideners in `veinminer/fabric/src/main/resources/veinminer.accesswidener` and `veinminer-client/fabric/src/main/resources/veinminerClient.accesswidener`.
- `veinminer-client/common/src/main/resources/veinminerClient.accesswidener` exists for compile-time access in the shared client project.
- NeoForge uses access transformers in `veinminer/neoforge/src/main/resources/META-INF/accesstransformer.cfg` and `veinminer-client/neoforge/src/main/resources/META-INF/accesstransformer.cfg`.
- Fabric client block outline handling uses `MixinLevelRenderer`; mouse-wheel shape/depth selection uses `MixinMouseHandler`.
- NeoForge client rendering uses loader events and platform render adapters (`NeoHUDRenderer`, `NeoShapeRouletteRenderer`).
- The enchantment addon uses the id namespace `veinminer_enchantment` in resources and code. Keep that spelling; the old hyphenated namespace is not valid for NeoForge.

## Product Lines

- V2 is the main codebase output: the Paper/Purpur/Folia plugin, Fabric/Quilt mod, NeoForge mod, client addon, and enchantment addon.
- V1 is the lightweight datapack line under `datapacks/veinminer/` and `datapacks/enchantment/`. It is configured through datapack functions and ships separately to Modrinth. V1 and V2 are independent; fixes usually do not transfer automatically.
