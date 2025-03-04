import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.paper")
    id("com.modrinth.minotaur")
}

description = properties["description"] as String

val gameVersion by properties

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("$gameVersion-R0.1-SNAPSHOT")

    // Kotlin libraries
    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")

    // Utility libraries (optional)
    val useBrigadier = properties["useBrigadier"] as String == "true"
    if (useBrigadier) {
        implementation(library("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")!!)
        implementation(library("dev.jorel:commandapi-bukkit-kotlin:9.7.0")!!)
    }

    library("de.miraculixx:kpaper:1.+")
}

//tasks {
//    assemble {
//        dependsOn(reobfJar)
//    }
//}

paper {
    main = "$group.veinminer.Veinminer"
    bootstrapper = "$group.veinminer.VeinminerBootstrapper"
    loader = "$group.veinminer.VeinminerLoader"
    generateLibrariesJson = true

    name = "Veinminer"
    website = "https://mutils.net"

    foliaSupported = true
    apiVersion = "1.20"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}
