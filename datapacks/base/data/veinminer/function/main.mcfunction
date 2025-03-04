# Check for each player
execute as @a run function veinminer:check

# Cooldown Handling
execute as @a run execute unless score @s cooldown matches 0.. run scoreboard players set @s cooldown 0
execute as @a[scores={cooldown=1..}] run scoreboard players remove @s cooldown 1
