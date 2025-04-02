scoreboard players set @s veinminer_const 0
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 1}}}}] run scoreboard players set @s veinminer_const 1
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 2}}}}] run scoreboard players set @s veinminer_const 2
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:fortune": 3}}}}] run scoreboard players set @s veinminer_const 3
execute if entity @s[nbt={SelectedItem:{components:{"minecraft:enchantments":{"minecraft:silk_touch": 1}}}}] run scoreboard players set @s veinminer_const 10

execute as @s[scores={mined_co=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/coal
execute as @s[scores={mined_di=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/diamond
execute as @s[scores={mined_go=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/gold
execute as @s[scores={mined_ir=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/iron
execute as @s[scores={mined_em=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/emerald
execute as @s[scores={mined_re=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/redstone
execute as @s[scores={mined_nq=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/nether_quartz
execute as @s[scores={mined_ng=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/nether_gold
execute as @s[scores={mined_la=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/lapis
execute as @s[scores={mined_cop=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/copper

execute as @s[scores={mined_co_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/coal_deep
execute as @s[scores={mined_di_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/diamond_deep
execute as @s[scores={mined_go_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/gold_deep
execute as @s[scores={mined_ir_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/iron_deep
execute as @s[scores={mined_em_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/emerald_deep
execute as @s[scores={mined_re_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/redstone_deep
execute as @s[scores={mined_la_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/lapis_deep
execute as @s[scores={mined_cop_d=1..}] at @e[type=item,limit=1,sort=nearest,nbt={Age:0s}] positioned ~ ~ ~ run function veinminer:miner/copper_deep

scoreboard players operation @s cooldown = default cooldown
