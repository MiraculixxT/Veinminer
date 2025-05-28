# Load data into temp storage
data modify storage veinminer:data temp0.list set from storage veinminer:data tools.pickaxe
data modify storage veinminer:data temp0.type set value "pickaxe"

# Start traversal
function veinminer:internal/check/_loop
