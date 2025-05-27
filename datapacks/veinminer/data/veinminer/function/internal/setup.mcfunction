#
# Loading Default Config
#

# Create scoreboards
scoreboard objectives add veinminer.settings dummy
scoreboard objectives add veinminer.cooldown dummy
scoreboard objectives add veinminer.silk dummy
scoreboard objectives add veinminer.enchantment dummy

# Default blocks & tools
data modify storage veinminer:data category set value {pickaxe: 1b, shovel: 1b, axe: 1b, hoe: 1b}
execute unless score init veinminer.settings matches 1.. run function veinminer:_reset

# Default settings
execute unless score sneak veinminer.settings matches 0.. run scoreboard players set sneak veinminer.settings 0
execute unless score disabled veinminer.settings matches 0.. run scoreboard players set disabled veinminer.settings 0
execute unless score default veinminer.cooldown matches 0.. run scoreboard players set default veinminer.cooldown 10

# Mark Veinminer as initilized
scoreboard players set init veinminer.settings 1

say Veinminer Loaded!
