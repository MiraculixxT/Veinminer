# Check fortune
$execute if score @s veinminer_const matches 1 if predicate veinminer:fortune1 run function veinminer:miner/use_fortune {block:$(block)}
$execute if score @s veinminer_const matches 2 if predicate veinminer:fortune2 run function veinminer:miner/use_fortune {block:$(block)}
$execute if score @s veinminer_const matches 2 if predicate veinminer:fortune2 run function veinminer:miner/use_fortune {block:$(block)}
$execute if score @s veinminer_const matches 3 if predicate veinminer:fortune3 run function veinminer:miner/use_fortune {block:$(block)}
$execute if score @s veinminer_const matches 3 if predicate veinminer:fortune3 run function veinminer:miner/use_fortune {block:$(block)}
$execute if score @s veinminer_const matches 3 if predicate veinminer:fortune3 run function veinminer:miner/use_fortune {block:$(block)}

# Check silktouch
$execute if score @s veinminer_const matches 10 run function veinminer:miner/use_silktouch {block:$(block),type:$(type)}
