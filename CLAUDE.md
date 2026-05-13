# CLAUDE.md

## Build & run

Kotlin/Gradle multi-project. Java 25 toolchain, Kotlin 2.3, Gradle wrapper (`./gradlew`).

- `./gradlew build` — build all (jars go `<module>/build/libs/`).
- `./gradlew :veinminer:veinminer-paper:build` / `:veinminer:veinminer-fabric:build` / `:veinminer:veinminer-neoforge:build` — build one base-mod loader.
- `./gradlew :veinminer-client:veinminer-client-fabric:build` / `:veinminer-client:veinminer-client-neoforge:build` — build the client addon for one loader.
- `./gradlew :veinminer-enchant:build` — build the enchantment fatjar.
- `./gradlew :veinminer:veinminer-paper:runServer` — start Paper dev server (via `xyz.jpenilla.run-paper`); `runFolia` also registered.
- `./gradlew :veinminer:veinminer-fabric:runServer` / `:veinminer-client:veinminer-client-fabric:runClient` — start via fabric-loom (each got `run/` dirs).
- `./gradlew :datapacks:zipAll` — zip V1 datapacks (`zipVeinminer`, `zipEnchantment` for single ones).
- `./gradlew :veinminer:veinminer-paper:modrinth` (or any module's `modrinth` task, via `com.modrinth.minotaur`) — publish. Set `modrinthToken` in `gradle.properties` or via `-PmodrinthToken=...`.

The doubled segments (`:veinminer:veinminer-fabric`) come from disambiguated leaf names in `settings.gradle.kts` — without it, both `:veinminer:fabric` and `:veinminer-client:fabric` resolved to the same `de.miraculixx:fabric` module ID and Gradle substituted one for the other (circular task graph). Same for `common`/`neoforge`.

No test source set anywhere — don't make up test commands.

Game/loader versions live in `gradle.properties` (`gameVersion`, `paperVersion`, `fabricSupportedVersions`, `fabricLoaderVersion`, `neoforgeVersion`, `fancyModLoaderVersion`, `silkVersion`, `adventureVersion`). Bump Minecraft = edit those, not per-module build files.

## Architecture

Gradle layout (see `settings.gradle.kts`):

```
core                     ← shared logic, settings model, packet IDs, update checker (applies fabric-loom for MC types)
veinminer/
 ├── common              ← base-mod common code: NetworkRouter, VeinMinerEvent, EventState, BaseConfigManager, VeinminerCommand, VeinminerPayload (applies fabric-loom; depends on :core)
 ├── paper               ← Paper/Folia plugin (paperweight; depends on :core + :veinminer:veinminer-common)
 ├── fabric              ← Fabric server+client mod (loom; `include`s :veinminer:veinminer-common which chains :core)
 └── neoforge            ← NeoForge base-mod (moddev; jarJar's :core + :veinminer:veinminer-common)
veinminer-client/
 ├── common              ← client-addon common code (loom; currently sourceless — client-shared logic still lives in :veinminer:veinminer-common's ClientNetworkRouter / ClientPlatformNetwork / ClientCallbacks)
 ├── fabric              ← Fabric client-only addon: hotkey + block highlight (depends on :veinminer:veinminer-fabric)
 └── neoforge            ← NeoForge client-only addon: hotkey + block highlight (depends on :veinminer:veinminer-neoforge)
veinminer-enchant        ← Separate plugin/mod registering the Veinminer enchantment (fatjar across loaders)
datapacks                ← Zips the legacy V1 datapacks shipped to Modrinth
```

`core` and `veinminer/common` are loader-agnostic Kotlin but **do** see Minecraft types (both apply `net.fabricmc.fabric-loom` and pull `minecraft("com.mojang:minecraft:$gameVersion")`) so abstractions over blocks/items/world live there. They still avoid Bukkit/Fabric-API specifics — those leak only into the loader-leaf modules.

`core` owns:
- `config.data.VeinminerSettings` / `VeinminerClientSettings` / `VeinminerSettingsOverride` — canonical settings model (kotlinx-serialization). `applyOverrides(isClient, group)` enforce precedence **default < client-installed < block-group override**. Any new tunable must thread through all three classes plus `applyOverrides`.
- `config.data.BlockGroup` — generic over block/item type `T`; loader modules construct with native types (`NamespacedKey` for Paper, `Identifier` for Fabric) and provide a custom serializer.
- `config.network.NetworkManager` + `C2SPackets` / `S2CPackets` — channel name `veinminer` and packet IDs (`join`, `mine`, `key`, `configuration`, `highlight`). All loader networking implementations register against these constants; client and server speak the same wire protocol.
- `config.UpdateManager` — Modrinth update check used by every loader's `Veinminer` entrypoint.

`veinminer/common` holds base-mod code shared across Paper/Fabric/NeoForge: `event/VeinMinerEvent.kt` (the canonical mining algorithm — break spread, drops, durability — used by NMS-based loaders), `event/EventState.kt` (loader-set state plug-in points), `network/{NetworkRouter,ClientNetworkRouter,PlatformNetwork,ClientPlatformNetwork,ServerCallbacks,ClientCallbacks,VeinminerPayload}.kt`, `config/{BaseConfigManager,ConfigSerializer,ParsedData}.kt`, `command/VeinminerCommand.kt`. Each loader leaf carries the same shape: `Veinminer.kt` entry point, `<Loader>PlatformNetwork.kt` + `<Loader>ServerCallbacks.kt`, `config/ConfigManager.kt` (extends `BaseConfigManager`) + `<Loader>ConfigSerializer.kt`, `command/<Loader>VeinminerCommand.kt`, `utils/<Loader>Host.kt`. Paper has its own Bukkit-API-based `VeinMinerEvent` impl in `veinminer-paper` — it does **not** use the common one. The Fabric and NeoForge `ConfigManager`s are sibling implementations sharing `BaseConfigManager`'s logic; keep their `ConfigSerializer`s in sync if you edit one.

Shared resources (icons, lang, default groups) live at top-level dirs and are wired into each leaf's resources via `sourceSets { main { resources.srcDirs("$rootDir/<dir>/") } }`:
- `commons/default_groups.json` — Paper + Fabric + NeoForge base mod
- `veinminer/assets/` — base-mod icon (Fabric + NeoForge + Paper leaves)
- `veinminer-client/assets/` — client-addon icon, lang, textures (Fabric + NeoForge client leaves)
Edit once; do not duplicate per leaf.

`buildSrc/src/main/kotlin/*.gradle.kts` = convention plugins applied via `` `kotlin-script` ``, `` `fabric-script` ``, `` `paper-script` ``, etc. Cross-cutting build changes (Kotlin version, JVM target, mod metadata `processResources` substitution for `fabric.mod.json` / `neoforge.mods.toml`) live there, not in module `build.gradle.kts`.

### Multi-loom dependency rule

Because `:core` and `:veinminer:veinminer-common` both apply fabric-loom, the Fabric loader leaf modules must take **one** path to `:core` to avoid a circular `remapJar` graph. Convention: each leaf consumes only its direct parent (e.g. `:veinminer:veinminer-fabric` `include`s `:veinminer:veinminer-common`; `:veinminer-client:veinminer-client-fabric` depends on `:veinminer:veinminer-fabric`); the chain carries `:core` transitively. Don't add a second direct `include(project(":core"))` from a Fabric leaf that already pulls it through `:veinminer:veinminer-common` / `:veinminer:veinminer-fabric`. The client-addon Fabric leaf re-declares `:core` and `:veinminer:veinminer-common` as `compileOnly` — they're JiJ'd into `:veinminer:veinminer-fabric` so this gives compile-time visibility without re-bundling.

NeoForge uses moddev's `jarJar(project(...))` instead of loom `include`. The base mod `:veinminer:veinminer-neoforge` `jarJar`s both `:core` and `:veinminer:veinminer-common`; the client addon `:veinminer-client:veinminer-client-neoforge` `compileOnly`s both and depends on `:veinminer:veinminer-neoforge` for runtime (same pattern as Fabric).

## Conventions worth knowing

- Permissions: defined as `const val` in `core/.../config/utils/Globals.kt` (`veinminer.toggle`, `veinminer.blocks`, `veinminer.settings`, `veinminer.use`, `veinminer.groups`, `veinminer.reload`). OP/cheats bypass them. Use these constants — don't inline strings.
- `usePermissions`, `useConfig`, `useBrigadier`, `foliaSupport` in `gradle.properties` are real toggles read by the convention plugins; flipping them changes the dependency graph.
- Fabric uses access-wideners (`veinminer/fabric/src/main/resources/veinminer.accesswidener`, plus a separate one for the client addon under `veinminer-client/fabric`). Touching private MC fields means editing those files, not adding mixins.
- NeoForge uses access transformers (`veinminer/neoforge/src/main/resources/META-INF/accesstransformer.cfg`, ditto for the client addon). Wired into the build via `neoForge { accessTransformers.from(file(...)) }` in each leaf's `build.gradle.kts`. Current entries: `ServerPlayer.server` (base mod), `RenderPipelines.register` (client addon — Fabric Loom auto-widens this; NeoForge needs it explicit).
- NeoForge block-outline rendering uses `RenderLevelStageEvent.AfterOpaqueBlocks` + `AfterTranslucentBlocks` listeners on the game bus; Fabric uses a mixin on `LevelRenderer.renderBlockOutline` (`veinminer-client/fabric/src/main/java/.../mixin/MixinLevelRenderer.java`). Same `BlockHighlightingRenderer` body, different hook.
- NeoForge has no first-class permissions API; the `Permissions.install` shim in `command/NeoForgeVeinminerCommand.kt` checks `CommandSourceStack.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))`. Fabric uses `me.lucko:fabric-permissions-api`.
- Paper module uses `paperweight` with `MOJANG_PRODUCTION` reobf; the Brigadier integration is via CommandAPI shaded into the plugin (`dev.jorel:commandapi-paper-shade`), not the Paper Brigadier API directly.
- Paper module's `paper { main = ... }` and the enchant module's `paper { main = ... }` use the `de.eldoria.plugin-yml.paper` plugin to generate `paper-plugin.yml` at build time — there is no checked-in `plugin.yml`.
- All loader implementations of the network layer share the same packet IDs declared in `core`'s `NetworkManager`; the server-side dispatch lives in `:veinminer:veinminer-common`'s `NetworkRouter` (loader leaves only implement `PlatformNetwork` + `ServerCallbacks`). The client-side mirror lives in the same module as `ClientNetworkRouter`/`ClientPlatformNetwork`/`ClientCallbacks` — client-addon leaves implement just `ClientPlatformNetwork`. If you change a packet's payload, update its data class in `core` and re-run all builds; no per-loader edits should be needed.

## Two product lines, don't confuse them

- **V2** (this codebase's main output) — the Fabric/NeoForge mods and Paper plugin.
- **V1** — the lightweight datapacks under `datapacks/veinminer/` and `datapacks/enchantment/`, configured purely via `/function veinminer:_config`. They have their own versioning (see `datapacks/build.gradle.kts`) and ship separately to Modrinth. The two are independent — a V2 fix usually does not apply to V1.

## Ongoing rewrite

Tracker:
- [x] New module layout (`veinminer/{common,fabric,paper,neoforge}`, `veinminer-client/{common,fabric,neoforge}`)
- [x] NeoForge build pipeline filled in (`:veinminer:veinminer-neoforge`, `:veinminer-client:veinminer-client-neoforge`)
- [ ] Further move shared logic from loader leaves into common modules (specially client)
