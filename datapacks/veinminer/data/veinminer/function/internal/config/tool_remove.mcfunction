#
# Arguments:    index, category[pickaxe,axe,shovel,hoe], namespace, id
#
$data remove storage veinminer:data tools.$(category)[$(index)]
$scoreboard objectives remove veinminer.t.$(namespace).$(id)
$function veinminer:tool_edit {category:$(category)}
