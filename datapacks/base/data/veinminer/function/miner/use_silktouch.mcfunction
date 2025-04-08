# Apply silktouch

$playsound minecraft:block.$(type).break block @a ~ ~ ~ 1 0.8
setblock ~ ~ ~ air
$summon item ~ ~ ~ {PickupDelay:10,Item:{id:"minecraft:$(block)",Count:1b}}
$particle minecraft:block{block_state:$(block)} ~ ~0.35 ~ 0.35 0.35 0.35 1 40
