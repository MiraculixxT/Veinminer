execute if score disabled veinminer.settings matches 1.. run return run tellraw @s {"text": "Veinminer is already disabled!", color: "red"}

scoreboard players set disabled veinminer.settings 1
tellraw @s [{text: "Veinminer is now disabled! Use "}, {"text": "/function veinminer:_enable", underlined: true}, {text: " to enabled it again!", underlined: false}]
