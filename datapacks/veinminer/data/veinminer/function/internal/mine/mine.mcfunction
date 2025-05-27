# Apply enchantment
execute unless score @s veinminer.silk matches 0 run function veinminer:internal/mine/enchantments

# Destroy current block
setblock ~ ~ ~ air destroy

# Recursion to affect whole vein
execute positioned ~ ~ ~1 run function veinminer:internal/mine/try with storage veinminer:data temp.current
execute positioned ~ ~ ~-1 run function veinminer:internal/mine/try with storage veinminer:data temp.current
execute positioned ~1 ~ ~ run function veinminer:internal/mine/try with storage veinminer:data temp.current
execute positioned ~-1 ~ ~ run function veinminer:internal/mine/try with storage veinminer:data temp.current
execute positioned ~ ~1 ~ run function veinminer:internal/mine/try with storage veinminer:data temp.current
execute positioned ~ ~-1 ~ run function veinminer:internal/mine/try with storage veinminer:data temp.current
