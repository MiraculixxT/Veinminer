plugins {
    id("net.fabricmc.fabric-loom")
    id("io.github.dexman545.outlet")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven {
        name = "pauli.fyiReleases"
        url = uri("https://repo.pauli.fyi/releases")
    }
}

val transitiveInclude: Configuration by configurations.creating {
    exclude(group = "com.mojang")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
}

dependencies {
    val gameVersion: String by properties
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String

    //
    // Fabric configuration
    //
    minecraft("com.mojang:minecraft:$gameVersion")
    println("Game Version: $gameVersion\nSupported Versions: ${outlet.mcVersionRange}")
    println("FabricLoader: ${outlet.loaderVersion()}\nFabricAPI: ${outlet.fapiVersion()}")
    implementation("net.fabricmc:fabric-loader:${outlet.loaderVersion()}")
    implementation("net.fabricmc.fabric-api:fabric-api:${outlet.fapiVersion()}")

    //
    // Kotlin libraries
    //
    val flkVersion = outlet.latestModrinthModVersion("fabric-language-kotlin", outlet.mcVersions())
    println("Fabric Language Kotlin: $flkVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$flkVersion")

    //
    // Permissions configuration
    //
    val fabricPermAPI by properties // doesn't update their version tags good enough for auto...
    implementation(include("me.lucko:fabric-permissions-api:$fabricPermAPI")!!)

    // Add all non-mod dependencies to the jar
    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks.processResources {
    val modrinthSlug = properties["modrinthProjectId"] as? String ?: properties["modid"] as String
    val expansion = mapOf(
        "modid" to properties["modid"] as String,
        "version" to properties["version"] as String,
        "name" to properties["projectName"] as String,
        "description" to properties["description"],
        "author" to properties["author"] as String,
        "license" to properties["licence"] as String,
        "modrinth" to modrinthSlug,
        "mcversion" to outlet.mcVersionRange,
        "loaderVersion" to outlet.loaderVersion()
    )
    inputs.properties(expansion)
    filesMatching("fabric.mod.json") {
        expand(expansion)
    }
}
