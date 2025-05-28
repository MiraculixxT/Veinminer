# Apply enchantment
execute unless score @s veinminer.silk matches 0 run function veinminer:internal/mine/enchantments

# Destroy current block
execute unless block ~ ~ ~ air run setblock ~ ~ ~ air destroy

# Recursion to affect whole vein
function veinminer:internal/mine/check_aligning
