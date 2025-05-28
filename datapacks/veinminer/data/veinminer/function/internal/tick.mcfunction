# Check if disabled
execute if score disabled veinminer.settings matches 1 run return 0

# Check for block breaking
function veinminer:internal/check/_general

# Cooldown Handling
execute as @a run execute unless score @s veinminer.cooldown matches 0.. run scoreboard players set @s veinminer.cooldown 0
execute as @a[scores={veinminer.cooldown=1..}] run scoreboard players remove @s veinminer.cooldown 1
