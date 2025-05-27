#
# Arguments:    namespace, id, category[pickaxe,axe,shovel,hoe]
#

# Check if valid input
$execute unless data storage veinminer:data category.$(category) run return run function veinminer:internal/config/invalid_category {category:"$(category)"}

# Add tool
$data modify storage veinminer:data tools.$(category) append value {"namespace":"$(namespace)", "id":"$(id)"}
$scoreboard objectives add veinminer.t.$(namespace).$(id) minecraft.used:$(namespace).$(id)
$scoreboard players reset @a veinminer.t.$(namespace).$(id)

$tellraw @s [{"text": "The tool '$(namespace):$(id)' was added to category $(category) and can now veinmine all blocks inside this category\n"},{"text":"Use /function veinminer:tool_edit {category: $(category)} to modify"}]
