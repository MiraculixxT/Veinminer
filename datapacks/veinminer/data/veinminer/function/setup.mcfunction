# Normal Ores
scoreboard objectives add mined_co minecraft.mined:minecraft.coal_ore
scoreboard objectives add mined_di minecraft.mined:minecraft.diamond_ore
scoreboard objectives add mined_go minecraft.mined:minecraft.gold_ore
scoreboard objectives add mined_ir minecraft.mined:minecraft.iron_ore
scoreboard objectives add mined_em minecraft.mined:minecraft.emerald_ore
scoreboard objectives add mined_re minecraft.mined:minecraft.redstone_ore
scoreboard objectives add mined_la minecraft.mined:minecraft.lapis_ore
scoreboard objectives add mined_nq minecraft.mined:minecraft.nether_quartz_ore
scoreboard objectives add mined_ng minecraft.mined:minecraft.nether_gold_ore
scoreboard objectives add mined_cop minecraft.mined:minecraft.copper_ore

# Deepslate Ores
scoreboard objectives add mined_co_d minecraft.mined:minecraft.deepslate_coal_ore
scoreboard objectives add mined_di_d minecraft.mined:minecraft.deepslate_diamond_ore
scoreboard objectives add mined_go_d minecraft.mined:minecraft.deepslate_gold_ore
scoreboard objectives add mined_ir_d minecraft.mined:minecraft.deepslate_iron_ore
scoreboard objectives add mined_em_d minecraft.mined:minecraft.deepslate_emerald_ore
scoreboard objectives add mined_re_d minecraft.mined:minecraft.deepslate_redstone_ore
scoreboard objectives add mined_la_d minecraft.mined:minecraft.deepslate_lapis_ore
scoreboard objectives add mined_cop_d minecraft.mined:minecraft.deepslate_copper_ore

# Internal
scoreboard objectives add settings dummy
scoreboard objectives add cooldown dummy
scoreboard objectives add veinminer_const dummy
scoreboard objectives add veinminer_silk dummy
scoreboard objectives add flex_pick minecraft.used:minecraft.netherite_pickaxe
scoreboard objectives add dia_pick minecraft.used:minecraft.diamond_pickaxe
scoreboard objectives add iron_pick minecraft.used:minecraft.iron_pickaxe
scoreboard objectives add gold_pick minecraft.used:minecraft.golden_pickaxe
scoreboard objectives add stone_pick minecraft.used:minecraft.stone_pickaxe
scoreboard objectives add wood_pick minecraft.used:minecraft.wooden_pickaxe

# Addons
scoreboard objectives add veinminer.enchantment dummy

execute unless score hunger settings matches 0.. run scoreboard players set hunger settings 0
execute unless score default cooldown matches 0.. run scoreboard players set default cooldown 10
execute unless score wood_pick settings matches 0.. run scoreboard players set wood_pick settings 1
execute unless score stone_pick settings matches 0.. run scoreboard players set stone_pick settings 1
execute unless score iron_pick settings matches 0.. run scoreboard players set iron_pick settings 1
execute unless score gold_pick settings matches 0.. run scoreboard players set gold_pick settings 1
execute unless score dia_pick settings matches 0.. run scoreboard players set dia_pick settings 1
execute unless score flex_pick settings matches 0.. run scoreboard players set flex_pick settings 1

scoreboard players set 0 veinminer_const 0
scoreboard players set 1 veinminer_const 1
scoreboard players set 2 veinminer_const 2
scoreboard players set 3 veinminer_const 3


say Veinminer Loaded!
