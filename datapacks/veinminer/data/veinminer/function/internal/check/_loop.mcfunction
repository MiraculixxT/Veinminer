# Pop last
data modify storage veinminer:data temp.current set from storage veinminer:data temp.list[-1]
data remove storage veinminer:data temp.list[-1]

# Use current
function veinminer:internal/check/_loopi with storage veinminer:data temp.current

# Loop if not empty
execute store result score list veinminer.settings run data get storage veinminer:data temp.list
execute if score list veinminer.settings matches 1.. run function veinminer:internal/check/_loop
