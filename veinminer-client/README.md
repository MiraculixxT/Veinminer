![banner](https://cdn.modrinth.com/data/cached_images/1aca4c6d88e1b7701c06596e78e51b229eea2395.png)

<p align="center">
  <a href="https://ko-fi.com/miraculixx/donate"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg" alt="Modrinth" style=""></a>
  <a href="https://modrinth.com/project/veinminer-enchantment"><img src="https://cdn.modrinth.com/data/cached_images/ea836ed1714c4f065aca5466c99c9e7d8f5baeff.png" alt="Ko-fi" style=""></a>
  <a href="https://modrinth.com/project/veinminer"><img src="https://cdn.modrinth.com/data/cached_images/625146681182f85ef448f5c910e4cd28ec6cf604.png" alt="Curseforge" style=""></a>
</p>

## ⛏️ Veinminer Hotkey

Addon for the popular [Veinminer](https://modrinth.com/project/veinminer) mod/plugin that adds a custom hotkey, veinmine preview & pattern control. As long as this addon is installed, veinmining is only possible with the hotkey pressed (still respecting block rules and settings).

**Hold Hotkey**: `Y` or `Z` (can be changed in client controls, default is depending on your key layout)<br>
**Toggle Hotkey**: `unbound` (bind in client controls)

## ⚠ Important
[**Veinminer**](https://modrinth.com/project/veinminer) needs to be installed on the server too for this addon to work! If playing in single-player, install it on your client too.<br>
Works together with the [enchantment addon](https://modrinth.com/project/veinminer-enchantment) if installed.

![showcase](https://i.imgur.com/aJRZTBw.gif)

## ⚙ Settings
Pattern selection can be changed by holding the hotkey and **scrolling** up/down.<br>
Additionally holding **shift** allows changing the mining depth/width.

**Pattern Config Hotkey**: `O` (configure, add, remove patterns to your liking. Accessible via mod menu as well)
![settings ui preview](https://cdn.modrinth.com/data/dxa0Bm8m/images/c5f3c7ff3562caf2ec7e50fd6bef11a4f1f9d838.png)

---

The server can configure client behavior with the following options. The client mod will automatically disable itself if the server does not have Veinminer installed.

**Command**: `/veinminer settings client ...`

|           Setting           |                             Description                             | Default |
|:---------------------------:|:-------------------------------------------------------------------:|:-------:|
|           `allow`           |               If the use of the client mod is allowed               |  true   |
| `translucentBlockHighlight` |       If block highlighting should be visible through blocks        |  true   |
|      `allowAllBlocks`       | If hotkey user can veinmine every block (respecting other settings) |  false  |
|    `allowClientGroups`      | If patterns can combine their block filter into an own group (still limited to allowed blocks) |  true   |

Platform Notice: No reuploads or forks are allowed on Modrinth & CurseForge