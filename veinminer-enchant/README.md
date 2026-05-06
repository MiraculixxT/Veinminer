## ⛏️ Veinminer Enchantment

Addon for the popular [Veinminer](https://modrinth.com/project/veinminer) mod/plugin/data-pack that limits the use of veinmining to a new custom enchantment `Veinmine`! As long as this addon is installed, veinmining is only possible with the enchantment (still respecting block rules and settings).

## ⚠ Important
[**Veinminer**](https://modrinth.com/project/veinminer) needs to be installed too for this addon to work!

![Veinmine Enchant on Pickaxe](https://cdn.modrinth.com/data/cached_images/1228e4a9540a344e54ac41e6b33a9bb1a63ea33b.png)

## ⚙ Settings
For the data-pack version, open the zip file and navigate to `data/veinminer-enchantment/enchantment/veinminer.json`. For fabric version, open the jar file with a zip program like winrar or 7zip and navigate to the same file as for data-packs.

|      ID      | Default |                          Description                          |
|:------------:|:-------:|:-------------------------------------------------------------:|
|   `weight`   |    1    |   How often the enchantment appears (higher -> more common)   |
|  `min_cost`  |   15    |        At which level the enchantment appears in table        |
|  `max_cost`  |   65    |    At which level the enchantment stops appearing in table    |
| `anvil_cost` |    7    | How much level combining in anvil costs (stacked with others) |

The enchantment name is defined by a translatable key `enchantment.veinmine` and defaults to `Veinmine`.
