#
# Arguments:    namespace, id, category[pickaxe,axe,shovel,hoe]
#

# Check if valid input
$execute unless data storage veinminer:data category.$(category) run return run function veinminer:internal/config/invalid_category {category:"$(category)"}

# Add block
$data modify storage veinminer:data blocks.$(category) append value {"namespace":"$(namespace)", "id":"$(id)"}
$scoreboard objectives add veinminer.b.$(namespace).$(id) minecraft.broken:$(namespace).$(id)
$scoreboard players reset @a veinminer.b.$(namespace).$(id)

$tellraw @s [{"text": "The block '$(namespace):$(id)' was added to category $(category) and is veinmineable with all tools inside this category\n"},{"text":"Use /function veinminer:block_edit {category: $(category)} to modify"}]
