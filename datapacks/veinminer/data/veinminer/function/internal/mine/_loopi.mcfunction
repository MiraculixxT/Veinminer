# 
# Argument:     namespace, id
#
#$execute if entity @s[scores={veinminer.b.$(namespace).$(id)=1..}] run tellraw @a " - CHECK HIT: $(namespace):$(id)"
$execute if entity @s[scores={veinminer.b.$(namespace).$(id)=1..}] anchored eyes positioned ^ ^ ^1.5 at @n[type=item,nbt={Age:0s},distance=..5.0] run function veinminer:internal/mine/check_aligning
scoreboard players operation @s veinminer.cooldown = default veinminer.cooldown
