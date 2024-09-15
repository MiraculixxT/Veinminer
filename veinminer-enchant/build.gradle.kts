import dex.plugins.outlet.v2.util.ReleaseType
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
    id("io.papermc.paperweight.userdev")
    id("net.minecrell.plugin-yml.paper")
}

val gameVersion by properties

dependencies {
    paperweight.paperDevBundle("$gameVersion-R0.1-SNAPSHOT")
}

modrinth {
    uploadFile.set(tasks.remapJar)
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
    })
    dependencies {
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
        required.project("fabric-api")
        required.project("fabric-language-kotlin")

        val useConfig = properties["useConfig"] as String == "true"
        if (useConfig) {
            optional.project("cloth-config")
        }
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
    apiVersion = "1.19"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP

    serverDependencies {
        register("Veinminer") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}
