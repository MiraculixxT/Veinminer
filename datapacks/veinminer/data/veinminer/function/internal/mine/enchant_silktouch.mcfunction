# Apply silktouch
setblock ~ ~ ~ air destroy
$kill @n[type=item,distance=..0.9,nbt={Item:{id:"$(namespace):$(id)",count:1}}]
$summon item ~ ~ ~ {PickupDelay:10,Item:{id:"$(namespace):$(id)",count:1}}
