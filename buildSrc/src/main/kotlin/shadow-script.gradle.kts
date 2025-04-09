plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == properties["group"] as String
            }
        }
    }
    assemble {
        dependsOn(shadowJar)
    }
}