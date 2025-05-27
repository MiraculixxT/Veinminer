execute if score disabled veinminer.settings matches 0 run return run tellraw @s {"text": "Veinminer is already enabled!", color: "red"}

scoreboard players set disabled veinminer.settings 0
tellraw @s [{text: "Veinminer is now enabled!"}]
