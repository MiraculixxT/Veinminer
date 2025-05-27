# Pop last
data modify storage veinminer:data temp2.current set from storage veinminer:data temp2.list[-1]
data remove storage veinminer:data temp2.list[-1]

# Use current
scoreboard players remove config.count veinminer.settings 1
execute store result storage veinminer:data temp2.index int 1 run scoreboard players get config.count veinminer.settings
function veinminer:internal/config/_loopi with storage veinminer:data temp2

# Loop if not empty
execute store result score list veinminer.settings run data get storage veinminer:data temp2.list
execute if score list veinminer.settings matches 1.. run function veinminer:internal/config/_loop
