plugins {
    `kotlin-script`
    `publish-script`
    id("net.fabricmc.fabric-loom")
}

dependencies {
    val gameVersion: String by properties
    minecraft("com.mojang:minecraft:$gameVersion")
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
}

loom {
    accessWidenerPath = file("src/main/resources/veinminerClient.accesswidener") // only compile time
}
modrinth {
    projectId = properties["modrinthClientId"] as String
    syncBodyFrom = rootProject.file("veinminer-client/README.md").readText()
}
