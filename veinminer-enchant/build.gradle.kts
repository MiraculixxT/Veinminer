import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `kotlin-script`
    `fabric-script`
//    id("io.papermc.paperweight.userdev")
    id("de.eldoria.plugin-yml.paper")
}

val paperVersion by properties

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(include(project(":core"))!!)
    //paperweight.paperDevBundle("$paperVersion")
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
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
