#
# Run:          as server
# Condition:    none
#

scoreboard players set c veinminer.settings 0

# Check for pickaxes
execute store result score c veinminer.settings run data get storage veinminer:data tools.pickaxe
execute unless score c veinminer.settings matches 0 run function veinminer:internal/check/pickaxe

# Check for shovels
execute store result score c veinminer.settings run data get storage veinminer:data tools.shovel
execute unless score c veinminer.settings matches 0 run function veinminer:internal/check/shovel

# Check for axes
execute store result score c veinminer.settings run data get storage veinminer:data tools.axe
execute unless score c veinminer.settings matches 0 run function veinminer:internal/check/axe

# Check for hoes
execute store result score c veinminer.settings run data get storage veinminer:data tools.hoe
execute unless score c veinminer.settings matches 0 run function veinminer:internal/check/hoe
