![banner](https://cdn.modrinth.com/data/cached_images/1aca4c6d88e1b7701c06596e78e51b229eea2395.png)

<p align="center">
  <a href="https://ko-fi.com/miraculixx/donate"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg" alt="Modrinth" style=""></a>
  <a href="https://modrinth.com/project/veinminer-client"><img src="https://cdn.modrinth.com/data/cached_images/9badc43c5a3708f90d24088df4f37c03acc943b8.png" alt="Ko-fi" style=""></a>
  <a href="https://modrinth.com/project/veinminer"><img src="https://cdn.modrinth.com/data/cached_images/625146681182f85ef448f5c910e4cd28ec6cf604.png" alt="Curseforge" style=""></a>
</p>

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

Platform Notice: No reuploads or forks are allowed on Modrinth & CurseForge