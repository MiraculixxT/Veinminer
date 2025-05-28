# Pop last
data modify storage veinminer:data temp1.current set from storage veinminer:data temp1.list[-1]
data remove storage veinminer:data temp1.list[-1]

# Use current
function veinminer:internal/mine/_loopi with storage veinminer:data temp1.current

# Loop if not empty
execute store result score l1 veinminer.settings run data get storage veinminer:data temp1.list
execute if score l1 veinminer.settings matches 1.. run function veinminer:internal/mine/_loop
