# 
# Argument: namespace, id, type[block,tool], category, index
#
$tellraw @s [\
            {"text":" $(index).", "color":"white"},\
            {"text":" [-] ", "color":"red",\
                "hoverEvent":{"action":"show_text", "contents":[{"text":"Remove this entry (click)", "color":"red"}]},\
                "clickEvent":{"action":"run_command","value":"/function veinminer:internal/config/$(type)_remove {index:$(index),category:\"$(category)\",namespace:\"$(namespace)\",id:\"$(id)\"}"}\
            },\
            {"text":"$(namespace):$(id)", "color":"gray",\
                "hoverEvent":{"action":"show_item", "contents":{"id":"$(namespace):$(id)"}}\
            }\
           ]
