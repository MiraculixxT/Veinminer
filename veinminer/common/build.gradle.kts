plugins {
    `kotlin-script`
    `publish-script`
    id("net.fabricmc.fabric-loom-remap")
}

val loomExtension = extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
    add("mappings", loomExtension.officialMojangMappings())
    implementation(project(":core"))
}

modrinth {
    projectId = properties["modrinthId"] as String
    syncBodyFrom = rootProject.file("veinminer/README.md").readText()
}
