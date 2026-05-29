import de.miraculixx.gradle.curseForgePublish

plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
    mods {
        create("veinminer-client") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":veinminer-client:veinminer-client-common").sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-neoforge"))
    implementation(project(":veinminer-client:veinminer-client-common"))
}

tasks.jar {
    from(project(":veinminer-client:veinminer-client-common").sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
    changelog = properties["changelogClient"] as String
    loaders.addAll(buildList {
        add("neoforge")
    })
    dependencies {
        required.project("kotlin-lang-forge")
        required.project("veinminer")
    }
}

curseForgePublish {
    projectId.set(properties["curseforgeClientId"] as String)
    versionName.set("Veinminer Hotkey NeoForge - $version")
    changelog.set(properties["changelogClient"] as String)
    gameVersions.addAll(outlet.mcVersions())
    loaders.add("NeoForge")
    environments.set(listOf("Client"))
    requiredDependencies.addAll("kotlinlangforge", "veinminer-mod")
}
