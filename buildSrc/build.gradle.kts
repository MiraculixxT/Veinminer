plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.1.0"
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
    val kotlinVersion = "2.1.0"

    compileOnly(kotlin("gradle-plugin", kotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))
    compileOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    runtimeOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")

    // Fabric implementation
    implementation("net.fabricmc:fabric-loom:1.10-SNAPSHOT")

    // Paper implementation
    implementation(pluginDep("io.papermc.paperweight.userdev", "2.0.0-beta.17"))
    implementation(pluginDep("xyz.jpenilla.run-paper", "2.3.1"))
    implementation(pluginDep("de.eldoria.plugin-yml.paper", "0.7.+"))

    // Project configuration
    implementation(pluginDep("com.gradleup.shadow", "9.0.0-beta9"))
    implementation(pluginDep("com.modrinth.minotaur", "2.+"))
    implementation(pluginDep("io.github.dexman545.outlet", "1.6.+"))
}
