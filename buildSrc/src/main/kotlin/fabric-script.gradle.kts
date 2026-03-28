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
    outlet.mcVersionRange = properties["fabricDependencyVersions"] as String

    //
    // Fabric configuration
    //
    minecraft("com.mojang:minecraft:$gameVersion")
    //mappings(loom.officialMojangMappings())
//    println("FabricLoader: " + outlet.loaderVersion() + : + outlet.fapiVersion())
//    implementation("net.fabricmc:fabric-loader", outlet.loaderVersion())
//    implementation("net.fabricmc.fabric-api:fabric-api", outlet.fapiVersion())
    implementation("net.fabricmc:fabric-loader:0.18.4")
    implementation("net.fabricmc.fabric-api:fabric-api:0.144.0+26.1")

    //
    // Kotlin libraries
    //
    val flkVersion = outlet.latestModrinthModVersion("fabric-language-kotlin", outlet.mcVersions())
    implementation("net.fabricmc:fabric-language-kotlin:$flkVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")

    //
    // Silk configuration (optional)
    //
    val silkVersion = properties["silkVersion"] as String
    implementation("net.silkmc:silk-core:$silkVersion") // core utilities
    implementation("net.silkmc:silk-commands:$silkVersion") // easy command registration
    implementation("net.silkmc:silk-nbt:$silkVersion") // item simplification
    implementation("net.silkmc:silk-network:$silkVersion") // networking utilities


    //
    // Permissions configuration (optional)
    //
    val usePermissions = properties["usePermissions"] as String == "true"
    if (usePermissions) {
        implementation(include("me.lucko:fabric-permissions-api:0.7.0")!!)
    }

    //
    // Ingame configuration (optional)
    //
    val useConfig = properties["useConfig"] as String == "true"
    if (useConfig) {
        api("com.terraformersmc:modmenu:9.+")
        api("me.shedaniel.cloth:cloth-config-fabric:21.+") {
            exclude("net.fabricmc.fabric-api")
        }
        transitiveInclude(implementation("org.yaml:snakeyaml:2.2")!!)
    }


    // Add all non-mod dependencies to the jar
    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks.processResources {
    println("Version Range: " + outlet.mcVersionRange + " - ${properties["version"]}")
    filesMatching("fabric.mod.json") {
        val modrinthSlug = properties["modrinthProjectId"] as? String ?: properties["modid"] as String
        expand(
            mapOf(
                "modid" to properties["modid"] as String,
                "version" to properties["version"] as String,
                "name" to properties["projectName"] as String,
                "description" to properties["description"],
                "author" to properties["author"] as String,
                "license" to properties["licence"] as String,
                "modrinth" to modrinthSlug,
                "environment" to properties["environment"] as String,
                "mcversion" to outlet.mcVersionRange,
            )
        )
    }
}
