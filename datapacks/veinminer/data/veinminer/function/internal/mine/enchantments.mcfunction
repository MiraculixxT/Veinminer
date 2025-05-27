# Check fortune
execute if score @s veinminer.silk matches 1 if predicate veinminer:fortune1 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current
execute if score @s veinminer.silk matches 2 if predicate veinminer:fortune2 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current
execute if score @s veinminer.silk matches 2 if predicate veinminer:fortune2 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current
execute if score @s veinminer.silk matches 3 if predicate veinminer:fortune3 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current
execute if score @s veinminer.silk matches 3 if predicate veinminer:fortune3 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current
execute if score @s veinminer.silk matches 3 if predicate veinminer:fortune3 run function veinminer:internal/mine/enchant_fortune with storage veinminer:data temp.current

# Check silktouch
execute if score @s veinminer.silk matches 10 run function veinminer:internal/mine/enchant_silktouch with storage veinminer:data temp.current
