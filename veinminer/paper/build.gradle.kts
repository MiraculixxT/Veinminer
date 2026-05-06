import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
    `publish-script`
    id("io.github.dexman545.outlet")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":veinminer:veinminer-common"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}

// to have a runFolia next to the runServer task for easier testing
runPaper.folia.registerTask()

tasks.jar       { archiveClassifier.set("dev") }
tasks.shadowJar { archiveClassifier.set("")    }

modrinth {
    uploadFile.set(tasks.shadowJar)
    versionName = "Veinminer Plugin - $version"
    outlet.mcVersionRange = properties["paperSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    changelog = properties["changelog"] as String
    loaders.addAll(buildList {
        add("paper")
        add("purpur")
        add("folia")
    })
    dependencies { }
}
