#
# Arguments:    category[pickaxe,axe,shovel,hoe]
#

# Check if valid input
$execute unless data storage veinminer:data category.$(category) run return run function veinminer:internal/config/invalid_category {category:"$(category)"}

# Load data
$data modify storage veinminer:data temp.list set from storage veinminer:data tools.$(category)
$data modify storage veinminer:data temp.category set value "$(category)"
data modify storage veinminer:data temp.type set value "tool"
scoreboard players add config.count veinminer.settings 0

# Output
execute store result score c veinminer.settings run data get storage veinminer:data temp.list
$execute if score c veinminer.settings matches 0 run return run tellraw @s {"text": "The tool list for $(category) is empty!", "color": "red"}
tellraw @s [{"text":"\n\n>> ","color":"dark_gray"}, {"text":"Veinminer tools in category ","color":"white"}, {"text":"$(category)", "color":"green"}, {"text":"", "color":"white"},\
            {"text":"\n   [+] ","color":"green","click_event":{"action":"suggest_command","command":"/function veinminer:tool_add {category:\"$(category)\", namespace:\"\", id:\"\"}"},\
                                                  "hover_event":{"action":"show_text","value":{"text":"Add a new tool\nnamespace -> Mod ID (e.g. minecraft)\nid -> Item ID (e.g. iron_pickaxe)"}}\
            },\
            {"text":"Add Tool","color":"gray"}\
           ]
function veinminer:internal/config/_loop
