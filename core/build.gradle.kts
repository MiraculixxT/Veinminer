
plugins {
    `kotlin-script`
    `adventure-script`
    id("net.fabricmc.fabric-loom-remap")
}

val loomExtension = extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
    add("mappings", loomExtension.officialMojangMappings())
}
