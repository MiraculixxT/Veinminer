execute if score hunger settings matches 1.. run effect give @s minecraft:hunger 2 120 true

# Apply fortune
execute if block ~ ~ ~ nether_gold_ore unless score @s veinminer_const matches 0 run function veinminer:miner/check_fortune {block:"nether_gold_ore",type:"nether_gold_ore"}

# Destroy current block
execute if block ~ ~ ~ nether_gold_ore run setblock ~ ~ ~ air destroy

# Recursion to affect whole vein
execute positioned ~ ~ ~1 if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~ ~ ~-1 if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~1 ~ ~ if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~1 ~ ~1 if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~1 ~ ~-1 if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~-1 ~ ~ if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~ ~1 ~ if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
execute positioned ~ ~-1 ~ if block ~ ~ ~ nether_gold_ore run function veinminer:miner/nether_gold
