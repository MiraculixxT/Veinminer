plugins {
    `kotlin-script`
    `publish-script`
    id("net.fabricmc.fabric-loom")
}

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
}

modrinth {
    projectId = "none" // Route into void on accident publish
    syncBodyFrom = rootProject.file("veinminer-client/README.md").readText()
}
