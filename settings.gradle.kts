rootProject.name = "Veinminer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(":core")
include(":veinminer-paper")
include(":veinminer-fabric")
include(":veinminer-enchant")
include(":veinminer-client")
