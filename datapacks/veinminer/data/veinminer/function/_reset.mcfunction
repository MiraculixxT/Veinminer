# Reset blocks
data remove storage veinminer:data blocks

data modify storage veinminer:data blocks.shovel set value []
data modify storage veinminer:data blocks.axe set value []
data modify storage veinminer:data blocks.hoe set value []

function veinminer:block_add {namespace: "minecraft", id: "coal_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "diamond_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "gold_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "iron_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "emerald_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "redstone_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "lapis_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "copper_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_coal_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_diamond_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_gold_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_iron_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_emerald_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_redstone_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_lapis_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "deepslate_copper_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "nether_quartz_ore", category: "pickaxe"}
function veinminer:block_add {namespace: "minecraft", id: "nether_gold_ore", category: "pickaxe"}

# Reset tools
data remove storage veinminer:data tools

data modify storage veinminer:data tools.shovel set value []
data modify storage veinminer:data tools.axe set value []
data modify storage veinminer:data tools.hoe set value []

function veinminer:tool_add {namespace: "minecraft", id: "wooden_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "stone_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "iron_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "golden_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "diamond_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "netherite_pickaxe", category: "pickaxe"}
function veinminer:tool_add {namespace: "minecraft", id: "copper_pickaxe", category: "pickaxe"}

# Reset settings
scoreboard players set sneak veinminer.settings 0
scoreboard players set disabled veinminer.settings 0
scoreboard players set default veinminer.cooldown 10

tellraw @s {"text":"\n\n\n\nAll veinminer settings are resetted to the default values"}
