import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
    `publish-script`
    id("io.github.dexman545.outlet")
}

dependencies {
    implementation(project(":core"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}

modrinth {
    tasks.jar
    uploadFile.set("${layout.buildDirectory.asFile.get().path}\\libs\\${properties["projectName"]}-${properties["version"]}.jar")
    versionName = "Veinminer Plugin - ${properties["version"]}"
    outlet.mcVersionRange = properties["paperSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("paper")
        add("purpur")

        val foliaSupport = properties["foliaSupport"] as String == "true"
        if (foliaSupport) add("folia")
    })
    dependencies {
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
//        required.project("fabric-api")
    }

    changelog = "- Fix error caused by latest Paper API changes regarding brigadier commands"
}

tasks.jar {
    archiveBaseName = properties["projectName"] as String
}
