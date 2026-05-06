plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

dependencies {
    implementation(project(":core"))
    implementation(project(":veinminer:veinminer-common"))

    jarJar(project(":core"))
    jarJar(project(":veinminer:veinminer-common"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.jar)
    versionName = "Veinminer NeoForge - $version"
    outlet.mcVersionRange = properties["neoforgeSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("neoforge")
    })
    dependencies {
        required.project("kotlin-for-forge")
    }
}
