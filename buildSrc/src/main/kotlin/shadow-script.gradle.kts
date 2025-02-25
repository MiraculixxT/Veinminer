plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == properties["group"] as String || it.moduleGroup == "dev.jorel"
            }
        }
        relocate("dev.jorel.commandapi", "de.miraculixx.veinminer.commandapi")
    }
}