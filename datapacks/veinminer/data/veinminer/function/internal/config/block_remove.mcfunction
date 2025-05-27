#
# Arguments:    index, category[pickaxe,axe,shovel,hoe], namespace, id
#
$data remove storage veinminer:data blocks.$(category)[$(index)]
$scoreboard objectives remove veinminer.b.$(namespace).$(id)
$function veinminer:block_edit {category:$(category)}
