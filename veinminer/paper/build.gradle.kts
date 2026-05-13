plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
    `publish-script`
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
    projectId = properties["modrinthId"] as String
    versionName = "Veinminer Paper - $version"
    outlet.mcVersionRange = properties["paperSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("paper")
        add("purpur")
        add("folia")
    })
    dependencies { }
}
