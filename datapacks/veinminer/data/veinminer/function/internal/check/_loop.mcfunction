# Pop last
data modify storage veinminer:data temp0.current set from storage veinminer:data temp0.list[-1]
data remove storage veinminer:data temp0.list[-1]

# Use current
function veinminer:internal/check/_loopi with storage veinminer:data temp0.current

# Loop if not empty
execute store result score l0 veinminer.settings run data get storage veinminer:data temp0.list
execute if score l0 veinminer.settings matches 1.. run function veinminer:internal/check/_loop
