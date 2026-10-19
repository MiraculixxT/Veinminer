[![dono-badge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/miraculixx/donate)
[![veinminer-badge](https://cdn.modrinth.com/data/cached_images/625146681182f85ef448f5c910e4cd28ec6cf604.png)](https://modrinth.com/mod/veinminer)
[![client-badge](https://cdn.modrinth.com/data/cached_images/9badc43c5a3708f90d24088df4f37c03acc943b8.png)](https://modrinth.com/project/veinminer-client)

## ⛏️ Veinminer Enchantment

Addon for the popular [Veinminer](https://modrinth.com/project/veinminer) mod/plugin/data-pack that limits the use of veinmining to a new custom enchantment `Veinmine`! As long as this addon is installed, veinmining is only possible with the enchantment (still respecting block rules and settings).

## ⚠ Important
[**Veinminer**](https://modrinth.com/project/veinminer) needs to be installed too for this addon to work!

![Veinmine Enchant on Pickaxe](https://cdn.modrinth.com/data/cached_images/1228e4a9540a344e54ac41e6b33a9bb1a63ea33b.png)

## ⚙ Settings
**Fabric/Quilt/NeoForge/Paper/Folia**: use `/veinminer enchant <setting> [<new-value>]` (permission `veinminer.settings`) or edit `config/Veinminer/enchantmentSettings.json` (`plugins/Veinminer/...` for plugins).
Changes apply after a server restart or world rejoin.<br>
**DataPack**: open the zip file and navigate to `data/veinminer-enchantment/enchantment/veinminer.json`

|       Setting       |             Default             |                          Description                          |
|:-------------------:|:-------------------------------:|:-------------------------------------------------------------:|
|     `maxLevel`      |               `1`               |                 Max level of the enchantment                  |
|      `weight`       |               `1`               |   How often the enchantment appears (higher -> more common)   |
|      `minCost`      |              `15`               |        At which level the enchantment appears in table        |
|  `minCostPerLevel`  |               `0`               |             Added `minCost` per level above first             |
|      `maxCost`      |              `65`               |    At which level the enchantment stops appearing in table    |
|  `maxCostPerLevel`  |               `0`               |             Added `maxCost` per level above first             |
|     `anvilCost`     |               `7`               | How much level combining in anvil costs (stacked with others) |
|  `supportedItems`   | `#minecraft:enchantable/mining` |         Item or item tag that can get the enchantment         |
| `inEnchantingTable` |             `true`              |            Enchantment appears in enchanting table            |
|     `tradeable`     |             `true`              |              Villagers can trade the enchantment              |
|     `lootable`      |             `true`              |              Enchantment appears in random loot               |
|     `treasure`      |             `false`             |          Treasure enchantment (doubled trade price)           |

Veinminers `minEnchantLevel` setting (also as group/client override) defines the min enchantment level required to veinmine, e.g. ores only at level 2+.

The enchantment name is defined by a translatable key `enchantment.veinminer-enchantment.veinminer` and defaults to `Veinmine`.

---

> **Affordable** Servers? In this time?! Clicking below **supports me** and saves you **10%** + 2 Days **free** ♥️
> [<img src="https://cdn.modrinth.com/data/cached_images/d594c32a9208540624e534d88ff2e428fed4c6d2.png" width=95%>](https://voltra.gg/r/miraculixx) <br>
> #AD - Your support helps to keep this project alive :)

---