
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

tasks {
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == properties["group"] as String
            }
        }
    }
}