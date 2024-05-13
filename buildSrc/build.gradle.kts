plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.23"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://server.bbkr.space/artifactory/libs-release/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://maven.quiltmc.org/repository/release/")
}

dependencies {
    fun pluginDep(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"
    val kotlinVersion = "1.9.23"

    compileOnly(kotlin("gradle-plugin", kotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))
    compileOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    runtimeOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")

    // Fabric implementation
    implementation("net.fabricmc:fabric-loom:1.6-SNAPSHOT")

    // Paper implementation
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:1.7.1")
    implementation(pluginDep("xyz.jpenilla.run-paper", "2.2.4"))
    implementation(pluginDep("net.minecrell.plugin-yml.bukkit", "0.6.+"))

    // Project configuration
    implementation(pluginDep("com.github.johnrengelman.shadow", "8.1.1"))
    implementation(pluginDep("com.modrinth.minotaur", "2.+"))
    implementation(pluginDep("io.github.dexman545.outlet", "1.6.+"))
}
