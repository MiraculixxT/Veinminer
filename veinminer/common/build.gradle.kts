plugins {
    `kotlin-script`
    `publish-script`
    id("net.fabricmc.fabric-loom")
}

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
    implementation(project(":core"))
}

modrinth {
    projectId = properties["modrinthId"] as String
    syncBodyFrom = rootProject.file("veinminer/README.md").readText()
}
