[![dono-badge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/miraculixx/donate)
[![silk-badge](https://i.imgur.com/iSCVMEz.png)](https://modrinth.com/mod/silk)
[![fapi-badge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)


## ⛏️ Veinminer Hotkey

Addon for the popular [Veinminer](https://modrinth.com/project/veinminer) mod/plugin that adds a custom hotkey and veinmine preview. As long as this addon is installed, veinmining is only possible with the hotkey pressed (still respecting block rules and settings).

**Hold Hotkey**: `Y` or `Z` (can be changed in client controls, default is depending on your key layout)<br>
**Toggle Hotkey**: `unbound` (bind in client controls)

## ⚠ Important
[**Veinminer**](https://modrinth.com/project/veinminer) needs to be installed on the server too for this addon to work! If playing in single-player, install it on your client too.<br>
Works together with the [enchantment addon](https://modrinth.com/project/veinminer-enchantment) if installed.

![showcase](https://i.imgur.com/aJRZTBw.gif)

## ⚙ Settings
The client mod itself does not provide settings, but the server can configure client behavior with the following options. The client mod will automatically disable itself if the server (or client if using in single-player) does not have Veinminer installed.

**Command**: `/veinminer settings client ...`

|           Setting           |                             Description                             | Default |
|:---------------------------:|:-------------------------------------------------------------------:|:-------:|
|           `allow`           |               If the use of the client mod is allowed               |  true   |
| `translucentBlockHighlight` |       If block highlighting should be visible through blocks        |  true   |
|      `allowAllBlocks`       | If hotkey user can veinmine every block (respecting other settings) |  false  |
