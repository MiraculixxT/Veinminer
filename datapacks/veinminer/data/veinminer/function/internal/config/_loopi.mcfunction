# 
# Argument: current, type[block,tool], category, index
#

# Move data
$data merge storage veinminer:data {temp2:{current:{category:"$(category)", index:$(index), type: "$(type)"}}}
function veinminer:internal/config/display_entry with storage veinminer:data temp2.current
