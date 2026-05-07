plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-fabric"))
    implementation(include(project(":veinminer-client:veinminer-client-common"))!!)
}

loom {
    accessWidenerPath = file("src/main/resources/veinminerClient.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer-client/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.jar)
    projectId = properties["modrinthClientId"] as String
    versionName = "Veinminer Hotkey Fabric - $version"
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
    })
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        required.project("veinminer")
    }
}
