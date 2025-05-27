# 
# Argument:     namespace, id
#
$execute if entity @s[scores={veinminer.b.$(namespace).$(id)=1..}] anchored eyes positioned ^ ^ ^1.5 at @n[type=item,nbt={Age:0s},distance=..2.0] run function veinminer:internal/mine/try with storage veinminer:data temp.current
scoreboard players operation @s veinminer.cooldown = default veinminer.cooldown
