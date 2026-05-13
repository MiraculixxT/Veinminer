plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
    mods {
        create("veinminer") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":core").sourceSets.main.get())
            sourceSet(project(":veinminer:veinminer-common").sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":veinminer:veinminer-common"))
}

tasks.jar {
    from(project(":core").sourceSets["main"].output)
    from(project(":veinminer:veinminer-common").sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.jar)
    projectId = properties["modrinthId"] as String
    versionName = "Veinminer NeoForge - $version"
    outlet.mcVersionRange = properties["neoforgeSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("neoforge")
    })
    dependencies {
        required.project("kotlin-lang-forge")
    }
}
