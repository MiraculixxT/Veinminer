execute if score hunger settings matches 1.. run effect give @s minecraft:hunger 2 120 true

# Apply fortune
execute if block ~ ~ ~ iron_ore unless score @s veinminer_const matches 0 run function veinminer:miner/check_fortune {block:"iron_ore",type:"stone"}

# Destroy current block
execute if block ~ ~ ~ iron_ore run setblock ~ ~ ~ air destroy

# Recursion to affect whole vein
execute positioned ~ ~ ~1 if block ~ ~ ~ iron_ore run function veinminer:miner/iron
execute positioned ~ ~ ~-1 if block ~ ~ ~ iron_ore run function veinminer:miner/iron
execute positioned ~1 ~ ~ if block ~ ~ ~ iron_ore run function veinminer:miner/iron
execute positioned ~-1 ~ ~ if block ~ ~ ~ iron_ore run function veinminer:miner/iron
execute positioned ~ ~1 ~ if block ~ ~ ~ iron_ore run function veinminer:miner/iron
execute positioned ~ ~-1 ~ if block ~ ~ ~ iron_ore run function veinminer:miner/iron
