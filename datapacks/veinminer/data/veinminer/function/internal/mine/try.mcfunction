# 
# Argument:     namespace, id
#

# Check if target block is correct
$execute if block ~ ~ ~ $(namespace):$(id) run function veinminer:internal/mine/mine
