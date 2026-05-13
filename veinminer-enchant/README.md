[![dono-badge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/miraculixx/donate)
[![veinminer-badge](https://cdn.modrinth.com/data/cached_images/625146681182f85ef448f5c910e4cd28ec6cf604.png)](https://modrinth.com/mod/veinminer)
[![client-badge](https://cdn.modrinth.com/data/cached_images/9badc43c5a3708f90d24088df4f37c03acc943b8.png)](https://modrinth.com/project/veinminer-client)

## ⛏️ Veinminer Enchantment

Addon for the popular [Veinminer](https://modrinth.com/project/veinminer) mod/plugin/data-pack that limits the use of veinmining to a new custom enchantment `Veinmine`! As long as this addon is installed, veinmining is only possible with the enchantment (still respecting block rules and settings).

## ⚠ Important
[**Veinminer**](https://modrinth.com/project/veinminer) needs to be installed too for this addon to work!

![Veinmine Enchant on Pickaxe](https://cdn.modrinth.com/data/cached_images/1228e4a9540a344e54ac41e6b33a9bb1a63ea33b.png)

## ⚙ Settings
**DataPack**: open the zip file and navigate to `data/veinminer-enchantment/enchantment/veinminer.json`<br>
**Fabric/Quilt/NeoForge**: open the jar file (winrar, 7zip, ...) and navigate to `data/veinminer-enchantment/enchantment/veinminer.json`<br>
**Paper/Folia**: navigate to `plugins/veinminer/enchantmentSettings.json`

|      ID      | Default |                          Description                          |
|:------------:|:-------:|:-------------------------------------------------------------:|
|   `weight`   |    1    |   How often the enchantment appears (higher -> more common)   |
|  `min_cost`  |   15    |        At which level the enchantment appears in table        |
|  `max_cost`  |   65    |    At which level the enchantment stops appearing in table    |
| `anvil_cost` |    7    | How much level combining in anvil costs (stacked with others) |

The enchantment name is defined by a translatable key `enchantment.veinminer-enchantment.veinminer` and defaults to `Veinmine`.
