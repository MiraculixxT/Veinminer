import de.miraculixx.gradle.curseForgePublish

plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-fabric")) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
        }
    }
    implementation(project(":veinminer-client:veinminer-client-common")) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
        }
    }
    include(project(":veinminer-client:veinminer-client-common"))

    val modMenuVersion = outlet.latestModrinthModVersion("modmenu", outlet.mcVersions())
    modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")
}

val coreSourceSets = project(":core").extensions.getByType<SourceSetContainer>()
val commonSourceSets = project(":veinminer:veinminer-common").extensions.getByType<SourceSetContainer>()
val fabricSourceSets = project(":veinminer:veinminer-fabric").extensions.getByType<SourceSetContainer>()

dependencies {
    runtimeOnly(fabricSourceSets["main"].output)
}

loom {
    accessWidenerPath = file("src/main/resources/veinminerClient.accesswidener")
    mods {
        create("veinminer") {
            sourceSet(fabricSourceSets["main"])
            sourceSet(coreSourceSets["main"])
            sourceSet(commonSourceSets["main"])
        }
    }
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer-client/assets/")
    }
}

modrinth {
    uploadFile.set(tasks.remapJar)
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
    artifactTaskName.set("remapJar")
    versionName.set("Veinminer Hotkey Fabric - $version")
    changelog.set(properties["changelogClient"] as String)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll("Fabric", "Quilt")
    environments.set(listOf("Client"))
    requiredDependencies.addAll("fabric-api", "fabric-language-kotlin", "veinminer-mod")
}
