
plugins {
    `kotlin-script`
    `adventure-script`
    id("fabric-loom")
}

dependencies {
    val gameVersion: String by properties

    minecraft("com.mojang", "minecraft", gameVersion)
    mappings(loom.officialMojangMappings())
}
