plugins {
    `kotlin-script`
    id("net.fabricmc.fabric-loom")
}

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
    implementation(project(":core"))
}

tasks.jar {
    manifest {
        attributes("FMLModType" to "GAMELIBRARY")
    }
}
