# 
# Argument:     type, current{namespace, id}
#

$execute store result score c veinminer.settings run data get storage veinminer:data blocks.$(type)
execute if score c veinminer.settings matches 0 run return fail
$data modify storage veinminer:data temp1.list set from storage veinminer:data blocks.$(type)
#$tellraw @a " - CHECK $(type)"
function veinminer:internal/mine/_loop
