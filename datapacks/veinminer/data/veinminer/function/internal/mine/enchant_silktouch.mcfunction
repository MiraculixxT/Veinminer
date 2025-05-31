# Apply silktouch
setblock ~ ~ ~ air destroy
kill @n[type=item,distance=..0.9,nbt={Age:0s}]
$summon item ~ ~ ~ {PickupDelay:10,Item:{id:"$(namespace):$(id)",count:1},Age:1s}
