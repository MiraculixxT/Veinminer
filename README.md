## Veinminer
<!-- modrinth_exclude.start -->
### - [DOWNLOAD](https://modrinth.com/project/veinminer) -
<!-- modrinth_exclude.end -->

Mine a single ore to break the full vein of the same ore! 
Veinminer is a common feature in various modpacks or survival pvp game modes like UHC to speed up breaking whole veins.
Now you can use this feature as a datapack too!

**From 2.0.0** -> Add/remove blocks & more settings<br>
**From 1.2.1** -> Supports Silk Touch<br>
**From 1.2.0** -> Supports Fortune<br>
**From 1.1.0** -> Supports (Deepslate) Copper Ore<br>

## Customization & Settings
![](https://cdn-raw.modrinth.com/data/OhduvhIc/images/f4c0ad7fa3b8b579753c1f757e80151798717c68.gif)

**Veinminer comes in two different versions**

The DataPack (V1) version is a simple version that is usable on every server and world.

|                 Command                  | Permission  |                    Description                     |
|:----------------------------------------:|:-----------:|:--------------------------------------------------:|
| /function veinminer:settings/**pickaxe** | `OP/Cheats` |   Limit or grant the effect for certain pickaxes   |
| /function veinminer:settings/**general** | `OP/Cheats` | Change some general settings to balance the effect |

---
The Fabric & Paper (V2) version is a more advanced version that is only usable with Fabric/Quilt or Paper/PurPur servers.

|         Command         |      Permission      |                     Short Description                     |
|:-----------------------:|:--------------------:|:---------------------------------------------------------:|
|  /veinminer **blocks**  |  `veinminer.blocks`  |             Edit blocks that are veinmineable             |
|  /veinminer **toggle**  |  `veinminer.toggle`  |                Completely toggle Veinminer                |
| /veinminer **settings** | `veinminer.settings` |     Change settings like cooldown, max chain and more     |
|    *Using Vineminer*    |   `veinminer.use`    | If perm-restriction is active, this is needed to veinmine |

OP players will have all permissions. To manually grant permissions see [Luckperms](https://luckperms.net/)

## Some Advice
- To veinmine, your pickaxe must be able to mine the ore in normal conditions (unless disabled in V2)
- While Veinminer is running very lightweight, mining unnatural big veins can lag the client and the server through the amount of items (you can avoid this by lowering the maxChain value in the config)


If you need any help or want to share some ideas to add, just hop on our Discord ([dc.mutils.net](https://dc.mutils.net))
