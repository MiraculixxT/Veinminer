# 
# Argument:     namespace, id
#
$execute as @a[scores={veinminer.t.$(namespace).$(id)=1..}] at @s run function veinminer:internal/check/_mine with storage veinminer:data temp.current
