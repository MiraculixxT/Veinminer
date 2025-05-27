#
# Arguments:    category[pickaxe,axe,shovel,hoe]
#

# Check if valid input
$execute unless data storage veinminer:data category.$(category) run return run function veinminer:internal/config/invalid_category {category:"$(category)"}

# Load data
$data modify storage veinminer:data temp2.list set from storage veinminer:data tools.$(category)
$data modify storage veinminer:data temp2.category set value "$(category)"
data modify storage veinminer:data temp2.type set value "tool"
execute store result score c veinminer.settings run data get storage veinminer:data temp2.list
scoreboard players operation config.count veinminer.settings = c veinminer.settings

# Output
$tellraw @s [{"text":"\n\n>> ","color":"dark_gray"}, {"text":"Veinminer tools in category ","color":"white"}, {"text":"$(category) - ", "color":"green"}, {"score":{"name":"c","objective":"veinminer.settings"}, "color":"white"},\
            {"text":"\n    [+] ","color":"green","click_event":{"action":"suggest_command","command":"/function veinminer:tool_add {category:\"$(category)\", namespace:\"\", id:\"\"}"},\
                                                  "hover_event":{"action":"show_text","value":{"text":"Add a new tool\nnamespace -> Mod ID (e.g. minecraft)\nid -> Item ID (e.g. iron_pickaxe)"}}\
            },\
            {"text":"Add Tool","color":"gray"}\
           ]
execute if score c veinminer.settings matches 0 run return run tellraw @s {"text": " - (empty)", "color": "red"}

function veinminer:internal/config/_loop
