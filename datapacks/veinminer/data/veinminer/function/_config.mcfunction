#
# General Settings
#
tellraw @s [{"text":"\n>> ","color":"dark_gray"}, {"text":"General feature settings","color":"green"}]

execute if score sneak veinminer.settings matches 1.. run tellraw @s [{"text":" -> ","color":"white"}, {"text":"Sneaking Required - ","color":"gray"}, {"text":"[ON]","bold":true,"color":"green"}, {"text":"/","color":"gray","bold":false}, {"text":"[OFF]", "color":"red","clickEvent":{"action":"run_command","value":"/scoreboard players set sneak veinminer.settings 0"},"hoverEvent":{"action":"show_text","contents":{"text":"Deactivate sneaking requirement","color":"red"}}}]
execute if score sneak veinminer.settings matches ..0 run tellraw @s [{"text":" -> ","color":"white"}, {"text":"Sneaking Required - ","color":"gray"}, {"text":"[ON]","bold":false,"color":"green","clickEvent":{"action":"run_command","value":"/scoreboard players set sneak veinminer.settings 1"},"hoverEvent":{"action":"show_text","contents":{"text":"Activate sneaking requirement","color":"green"}}}, {"text":"/","color":"gray"}, {"text":"[OFF]","bold":true,"color":"red"}]

tellraw @s [{"text":" -> ","color":"white"}, {"text":"Veinminer cooldown - ","color":"gray"}, {"text":"[","color":"yellow"}, {"score":{"name":"default","objective":"veinminer.cooldown"},"color":"yellow"}, {"text":"]","color":"yellow"}, {"text":" (change)","color":"gray","clickEvent":{"action":"suggest_command","value":"/scoreboard players set default veinminer.cooldown "},"hoverEvent":{"action":"show_text","contents":{"text":"Click to change cooldown","color":"gold"}}}]

#
# Block Settings
#
tellraw @s [{"text":"\n>> ","color":"dark_gray"}, {"text":"Edit veinminable blocks","color":"green"}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Pickaxe Blocks","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit pickaxe blocks","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:block_edit {category:\"pickaxe\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Shovel Blocks","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit shovel blocks","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:block_edit {category:\"shovel\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Axe Blocks","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit axe blocks","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:block_edit {category:\"axe\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Hoe Blocks","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit hoe blocks","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:block_edit {category:\"hoe\"}"}}]

#
# Tool Settings
#
tellraw @s [{"text":"\n>> ","color":"dark_gray"}, {"text":"Edit tools that can veinmine","color":"green"}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Pickaxe Tools","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit pickaxes","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:tool_edit {category:\"pickaxe\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Shovel Tools","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit shovels","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:tool_edit {category:\"shovel\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Axe Tools","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit axes","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:tool_edit {category:\"axe\"}"}}]
tellraw @s [{"text":" -> ","color":"white"}, {"text":"Hoe Tools","color":"gray","hoverEvent":{"action":"show_text","contents":{"text":"Click to inspect or edit hoes","color":"yellow"}},"clickEvent":{"action":"run_command","value":"/function veinminer:tool_edit {category:\"hoe\"}"}}]
