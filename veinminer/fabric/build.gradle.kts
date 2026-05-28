import de.miraculixx.gradle.curseForgePublish

plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core"))!!)
    implementation(include(project(":veinminer:veinminer-common"))!!)
}

loom {
    accessWidenerPath = file("src/main/resources/veinminer.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.jar)
    projectId = properties["modrinthId"] as String
    versionName = "Veinminer Fabric - $version"
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
    })
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        optional.project("veinminer-client")
    }
}

curseForgePublish {
    projectId.set(properties["curseforgeId"] as String)
    versionName.set("Veinminer Fabric - $version")
    changelog.set(properties["changelog"] as String)
    gameVersions.addAll(outlet.mcVersions())
    loaders.add("Fabric")
    requiredDependencies.addAll(listOf("fabric-api", "fabric-language-kotlin"))
    optionalDependencies.add("veinminer-hotkey")
}
