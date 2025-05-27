# 
# Argument:     namespace, id
# Run:          as @a at @s
# Condition:    mined with tool
#

# Reset score
$scoreboard players reset @s veinminer.t.$(namespace).$(id)

# Check if enchantment addon is installed and active
execute if score init veinminer.enchantment matches 1 unless score @s veinminer.enchantment matches 1 run return fail
scoreboard players set @s veinminer.enchantment 0

# Check sneaking
execute if score sneaking veinminer.settings matches 1 unless predicate veinminer:sneaking run return fail

# Check cooldown
execute if entity @s[scores={veinminer.cooldown=1..}] run return fail

# Get tool enchantment
scoreboard players set @s veinminer.silk 0
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 1}}}}] run scoreboard players set @s veinminer.silk 1
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 2}}}}] run scoreboard players set @s veinminer.silk 2
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 3}}}}] run scoreboard players set @s veinminer.silk 3
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:silk_touch": 1}}}}] run scoreboard players set @s veinminer.silk 10

# Loop for mined ore
function veinminer:internal/check/perform_mine with storage veinminer:data temp
