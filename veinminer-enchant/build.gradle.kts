import de.miraculixx.gradle.curseForgePublish
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `kotlin-script`
    `publish-script`
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
    outlet.mcVersionRange = properties["gameVersion"] as String

    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("net.fabricmc:fabric-loader:${outlet.loaderVersion()}")
    compileOnly("net.neoforged.fancymodloader:loader:$fancyModLoaderVersion")
}

tasks.processResources {
    val tokens = mapOf(
        "version" to project.version.toString(),
        "license" to (properties["licence"] as String)
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

    name = "Veinminer_Enchantment"
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

modrinth {
    uploadFile.set(tasks.jar)
    projectId = properties["modrinthEnchantmentId"] as String
    versionName = "Veinminer Enchantment - $version"
    outlet.mcVersionRange = properties["enchantmentVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    changelog = properties["changelogEnchantment"] as String
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
        add("neoforge")
        add("paper")
        add("purpur")
        add("folia")
    })
    dependencies {
        required.project("veinminer")
    }

    syncBodyFrom = rootProject.file("veinminer-enchant/README.md").readText()
}

curseForgePublish {
    projectId.set(properties["curseforgeEnchantmentId"] as String)
    versionName.set("Veinminer Enchantment - $version")
    changelog.set(properties["changelogEnchantment"] as String)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(listOf("Fabric", "NeoForge"))
    environments.set(listOf("Server", "Client"))
    requiredDependencies.add("veinminer-mod")
}
