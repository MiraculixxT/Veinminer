[![dono-badge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/miraculixx/donate)
[![enchant-badge](https://cdn.modrinth.com/data/cached_images/ea836ed1714c4f065aca5466c99c9e7d8f5baeff.png)](https://modrinth.com/project/veinminer-enchantment)
[![client-badge](https://cdn.modrinth.com/data/cached_images/9badc43c5a3708f90d24088df4f37c03acc943b8.png)](https://modrinth.com/project/veinminer-client)

## ⛏️ Veinminer
Mine a single block to mine the whole vein!
Highly configurable and works everywhere, even on your vanilla server.
Make the tedious mining experience to something satisfying and fun!<br>
Veinminer works server side, so all clients are supported. Even cross loaders & versions for addons!

> ### Client Hotkey & Highlighting
>  Add the [**Hotkey Addon**](https://modrinth.com/datapack/veinminer-client) to only veinmine when pressing a hotkey & get a mining preview
> ### Use as Enchantment? <img src="https://cdn.modrinth.com/data/cached_images/53383ed6675f75253d1bf842f75aefd7adb93711.png" width=25>
>  Add the [**Veinminer Enchantment Addon**](https://modrinth.com/datapack/veinminer-enchantment) to limit veinmining to only enchanted tools!

## ⚙️ Customization & Settings
![](https://i.postimg.cc/ZR85KZJM/output.webp)

**Veinminer comes in two different versions**<br>
• **Version 2.x** -> Full customizable mod & plugin<br>
• **Version 1.x** -> Light DataPack with vanilla & snapshot support<br>
Both comes with preconfigured for ores and pickaxes. Read below how to extend it.

---
### Mod/Plugin Version (2.x)
The advanced (V2) version is a more powerful version that is only usable with Fabric/Quilt, Paper/PurPur/Folia & NeoForge servers.

|         Command         |      Permission      |                     Short Description                     |
|:-----------------------:|:--------------------:|:---------------------------------------------------------:|
|  /veinminer **blocks**  |  `veinminer.blocks`  |     Edit blocks that are veinmineable with every tool     |
|  /veinminer **groups**  |  `veinminer.groups`  |          Edit blocks & tools with precise rules           |
|  /veinminer **toggle**  |  `veinminer.toggle`  |              Toggle Veinminer (server wide)               |
| /veinminer **settings** | `veinminer.settings` |     Change settings like cooldown, max chain and more     |
|    *Using Veinminer*    |   `veinminer.use`    | If perm-restriction is active, this is needed to veinmine |

<details><summary>General Settings</summary>

To change a setting, enter `/veinminer settings ... [<new-value>]`. 
To check the current state, leave out the new value argument.

|        Setting         |                        Description                         | Default |
|:----------------------:|:----------------------------------------------------------:|:-------:|
|      `mustSneak`       |               Players must sneak to veinmine               | `false` |
|       `cooldown`       |    Time between players are able to veinmine (in ticks)    |  `20`   |
|        `delay`         |   Time between each automated block breaking (in ticks)    |   `1`   |
|       `maxChain`       | Max amount of blocks that can break from one source block  |  `100`  |
|   `needCorrectTool`    |     If blocks have a required tool, this must be used      | `true`  |
|     `searchRadius`     | Amount of blocks around connected blocks to search (max 5) |   `1`   |
| `permissionRestricted` | Only players with `veinminer.use` permission can veinmine  | `false` |
|    `mergeItemDrops`    |       All item drops are merged to the source block        | `false` |
|      `autoUpdate`      |  Check for updates and download new version if available   | `true`  |
|  `durabilityDecrease`  |        If each mined block should reduce durability        | `true`  |
| `miningSpeedModifier`  |     Increase block mining duration based on vein size      |  `0.0`  |

</details>
<details><summary>Block Groups - Advanced Settings</summary>

Block groups can hold multiple blocks together that will be treated like the same block.<br>
`/veinminer groups create <name> [<block1>] [<block2>]`

All blocks inside one group will be mined together. 
A block can be in multiple groups. New blocks can be added or removed from groups with the following commands:<br>
`/veinminer groups edit <name> add-block <block>`<br>
`/veinminer groups edit <name> remove-block <block>`

Groups can be limited to certain tools, for example group `wood` can only be mined by axes. 
If no tool is added to a group, all tools are allowed. If a block is in multiple groups, all tools from those groups are allowed.<br>
(If a block is in one unlimited tool group and one limited to axes, only axes work for this block)<br>
`/veinminer groups edit <name> add-tool <item>`<br>
`/veinminer groups edit <name> remove-tool <item>`

### Example (Logs)
```
1. /veinminer groups create Logs
2. /veinminer groups edit Logs add-block #minecraft:logs
3. /veinminer groups edit Logs add-tool #minecraft:axes
```

---

Block and item inputs allow block and item tags for easier editing.
For example, to add all logs enter `#minecraft:logs` into any block input.
Visit the wiki for a full list or look at a block with F3 under "looking at".

---

You can also modify the group file directly inside `.../veinminer/default_groups.json` (`config/...` for mods, `plugins/...` for plugins)

</details>
<details><summary>Setting Overrides - Too Advanced Settings</summary>

Overrides are optional settings, that will override the normal setting if the player is effected by it. 
See examples underneath for a better understanding, I don't blame you if you think "why...".

### Client Overrides
Those settings will be applied to all users that use the client addon (hotkey).<br>
E.g. you want to have `mustSneak` enabled in general, but disabled for hotkey users:
- `/veinminer settings mustSneak true` - True for all
- `/veinminer settings client override mustSneak false` - False override for hotkey users

### Group Overrides
Those settings will be applied to all users that mine a block from this specific group.<br>
Each group can have a different override, if a block is in multiple groups, only the override from the *oldest* group count (this behavior may see improvement later).<br>
E.g. you dont want that your sand & dirt group consumes durability:
- `/veinminer settings decreaseDurability true` - True for all blocks
- `/veinminer groups shovel edit override decreaseDurability false` - False override for this group

--- 

If both, client and group overrides, collide the group override takes priority (`normal < client < group`).
You can remove overrides with `/veinminer ... override unset <setting>`.

</details>

OP players will have all permissions. To manually grant permissions see [Luckperms](https://luckperms.net/) (NeoForge not supported yet).

---
### DataPack Version (1.x)
The DataPack (V1) version is a simple version that is usable on every server and world. 
Enter the config command to open an interactive chat menu to edit all settings.

|           Command           | Permission  |              Description              |
|:---------------------------:|:-----------:|:-------------------------------------:|
| /function veinminer:_config | `OP/Cheats` | Access the full settings menu in chat |

If you get prompted something like `{category:"pickaxe", namespace:"", id:""}`, you need to fill in the item/block namespace & ID (press F3+H to see them), e.g. `{category:"pickaxe", namespace:"minecraft", id:"sand"}` for `minecraft:sand`


## Some Advice
- To veinmine, your tool must be able to mine the block in normal conditions (unless disabled in V2)
- While Veinminer is running very lightweight, mining unnatural big veins can lag the client and the server through the amount of items


If you need any help or want to share some ideas to add, just hop on our Discord ([dc.mutils.net](https://dc.mutils.net))
