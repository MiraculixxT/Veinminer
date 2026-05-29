import de.miraculixx.gradle.curseForgePublish

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

    val modMenuVersion = outlet.latestModrinthModVersion("modmenu", outlet.mcVersions())
    compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
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
    changelog = properties["changelogClient"] as String
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

curseForgePublish {
    projectId.set(properties["curseforgeClientId"] as String)
    versionName.set("Veinminer Hotkey Fabric - $version")
    changelog.set(properties["changelogClient"] as String)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll("Fabric", "Quilt")
    environments.set(listOf("Client"))
    requiredDependencies.addAll("fabric-api", "fabric-language-kotlin", "veinminer-mod")
}
