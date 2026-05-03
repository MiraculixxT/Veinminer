import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `kotlin-script`
    id("de.eldoria.plugin-yml.paper")
}

val paperVersion by properties
val fabricLoaderVersion by properties
val fancyModLoaderVersion by properties

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    // Loader entrypoint surfaces only — never bundled, each loader provides its own at runtime.
    // paper-api also supplies SLF4J transitively for the logger used by the Fabric/NeoForge stubs.
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    compileOnly("net.neoforged.fancymodloader:loader:$fancyModLoaderVersion")
}

tasks.processResources {
    val tokens = mapOf(
        "version" to project.version.toString(),
        "license" to (properties["licence"] as String),
        "environment" to (properties["environment"] as String),
    )
    inputs.properties(tokens)
    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
        expand(tokens)
    }
}


paper {
    main = "$group.veinminerEnchant.paper.VeinminerEnchantment"
    bootstrapper = "$group.veinminerEnchant.paper.VeinminerEnchantmentBootstrapper"
    loader = "$group.veinminerEnchant.paper.VeinminerEnchantmentLoader"
    generateLibrariesJson = true

    name = "Veinminer-Enchantment"
    version = project.version.toString()
    description = "Adds a veinminer enchantment to the game"
    author = "Miraculixx"
    website = "https://mutils.net"

    foliaSupported = true
    apiVersion = "1.21"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP

    serverDependencies {
        register("Veinminer") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}
