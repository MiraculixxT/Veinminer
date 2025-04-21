# Check if enchantment addon is installed and active
execute if score init veinminer.enchantment matches 1 unless score @s veinminer.enchantment matches 1 run return fail
scoreboard players set @s veinminer.enchantment 0

tag @s[scores={mined_co=1..}] add mined_ore
tag @s[scores={mined_di=1..}] add mined_ore
tag @s[scores={mined_go=1..}] add mined_ore
tag @s[scores={mined_ir=1..}] add mined_ore
tag @s[scores={mined_em=1..}] add mined_ore
tag @s[scores={mined_re=1..}] add mined_ore
tag @s[scores={mined_nq=1..}] add mined_ore
tag @s[scores={mined_ng=1..}] add mined_ore
tag @s[scores={mined_la=1..}] add mined_ore
tag @s[scores={mined_cop=1..}] add mined_ore
tag @s[scores={mined_co_d=1..}] add mined_ore
tag @s[scores={mined_di_d=1..}] add mined_ore
tag @s[scores={mined_go_d=1..}] add mined_ore
tag @s[scores={mined_ir_d=1..}] add mined_ore
tag @s[scores={mined_em_d=1..}] add mined_ore
tag @s[scores={mined_re_d=1..}] add mined_ore
tag @s[scores={mined_la_d=1..}] add mined_ore
tag @s[scores={mined_cop_d=1..}] add mined_ore

execute if score flex_pick settings matches 1.. if entity @s[tag=mined_ore,scores={flex_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
execute if score dia_pick settings matches 1.. if entity @s[tag=mined_ore,scores={dia_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
execute if score iron_pick settings matches 1.. if entity @s[tag=mined_ore,scores={iron_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
execute if score gold_pick settings matches 1.. if entity @s[tag=mined_ore,scores={gold_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
execute if score stone_pick settings matches 1.. if entity @s[tag=mined_ore,scores={stone_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
execute if score wood_pick settings matches 1.. if entity @s[tag=mined_ore,scores={wood_pick=1..,cooldown=..0}] at @s anchored eyes positioned ^ ^ ^1.5 run function veinminer:veinminer
tag @s remove mined_ore

scoreboard players set @s mined_co 0
scoreboard players set @s mined_di 0
scoreboard players set @s mined_go 0
scoreboard players set @s mined_ir 0
scoreboard players set @s mined_em 0
scoreboard players set @s mined_re 0
scoreboard players set @s mined_nq 0
scoreboard players set @s mined_ng 0
scoreboard players set @s mined_la 0
scoreboard players set @s mined_cop 0
scoreboard players set @s mined_co_d 0
scoreboard players set @s mined_di_d 0
scoreboard players set @s mined_go_d 0
scoreboard players set @s mined_ir_d 0
scoreboard players set @s mined_em_d 0
scoreboard players set @s mined_re_d 0
scoreboard players set @s mined_la_d 0
scoreboard players set @s mined_cop_d 0
