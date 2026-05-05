rootProject.name = "Veinminer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.neoforged.net/releases/")
    }
}

// Common code for all modules
include(":core")

// Base mod impl (common for mod shared code)
include(":veinminer:common")
include(":veinminer:paper")
include(":veinminer:fabric")
include(":veinminer:neoforge")

// Client mod addon impl (common for shared code)
include(":veinminer-client:common")
include(":veinminer-client:fabric")
include(":veinminer-client:neoforge")

// Ambiguate names due to name resolving (veinminer:paper & veinminer-client:paper -> paper)
// To avoid Gradle mistaking modules give each a unique name
project(":veinminer:common").name = "veinminer-common"
project(":veinminer:paper").name = "veinminer-paper"
project(":veinminer:fabric").name = "veinminer-fabric"
project(":veinminer:neoforge").name = "veinminer-neoforge"
project(":veinminer-client:common").name = "veinminer-client-common"
project(":veinminer-client:fabric").name = "veinminer-client-fabric"
project(":veinminer-client:neoforge").name = "veinminer-client-neoforge"

// Enchantment addon (fatjar)
include(":veinminer-enchant")

// V1 DataPack builder
include(":datapacks")
