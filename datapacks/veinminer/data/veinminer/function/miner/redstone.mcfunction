execute if score hunger settings matches 1.. run effect give @s minecraft:hunger 2 120 true

# Apply fortune
execute if block ~ ~ ~ redstone_ore unless score @s veinminer_const matches 0 run function veinminer:miner/check_fortune {block:"redstone_ore",type:"stone"}

# Destroy current block
execute if block ~ ~ ~ redstone_ore run setblock ~ ~ ~ air destroy

# Recursion to affect whole vein
execute positioned ~ ~ ~1 if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
execute positioned ~ ~ ~-1 if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
execute positioned ~1 ~ ~ if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
execute positioned ~-1 ~ ~ if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
execute positioned ~ ~1 ~ if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
execute positioned ~ ~-1 ~ if block ~ ~ ~ redstone_ore run function veinminer:miner/redstone
