plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-neoforge"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer-client/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.jar)
    projectId = properties["modrinthClientId"] as String
    versionName = "Veinminer Hotkey NeoForge - $version"
    outlet.mcVersionRange = properties["neoforgeSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("neoforge")
    })
    dependencies {
        required.project("kotlin-for-forge")
        required.project("veinminer")
    }
}
