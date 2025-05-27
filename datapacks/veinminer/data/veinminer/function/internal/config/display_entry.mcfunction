# 
# Argument: namespace, id, type[block,tool], category, index
#
$tellraw @s [\
            {"text":" $(index).", "color":"white"},\
            {"text":" [-] ", "color":"red",\
                "hover_event":{"action":"show_text", "value":[{"text":"Remove this entry (click)", "color":"red"}]},\
                "click_event":{"action":"run_command","command":"/function veinminer:internal/config/$(type)_remove {index:$(index),category:"$(category)",namespace:"$(namespace)",id:"$(id)"}"}\
            },\
            {"text":"$(namespace):$(id)", "color":"gray",\
                "hover_event":{"action":"show_item", "id":"$(namespace):$(id)"}\
            }\
           ]
