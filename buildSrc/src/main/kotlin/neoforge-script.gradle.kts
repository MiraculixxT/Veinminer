plugins {
    id("net.neoforged.moddev")
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven {
        name = "KotlinForForge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

val neoforgeVersion: String by properties
val modid: String by properties
val kotlinForNeoforgeVersion: String by properties

neoForge {
    version = neoforgeVersion

    runs {
        register("server") {
            server()
        }

        register("client") {
            client()
        }
    }

    mods {
        register(modid) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForNeoforgeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")
}

tasks.processResources {
    val tokens = mapOf(
        "modid" to modid,
        "version" to project.version.toString(),
        "name" to (properties["projectName"] as String),
        "description" to (properties["description"] as? String ?: ""),
        "author" to (properties["author"] as String),
        "license" to (properties["licence"] as String),
        "neoforgeVersion" to neoforgeVersion,
    )
    inputs.properties(tokens)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(tokens)
    }
}