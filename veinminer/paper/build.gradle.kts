plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
    `publish-script`
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
}

val coreSourceSets = project(":core").extensions.getByType<SourceSetContainer>()
val commonSourceSets = project(":veinminer:veinminer-common").extensions.getByType<SourceSetContainer>()

tasks.shadowJar {
    dependsOn(":core:classes", ":veinminer:veinminer-common:classes")
    from(coreSourceSets.named("main").map { it.output })
    from(commonSourceSets.named("main").map { it.output })
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
