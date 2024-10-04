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
    uploadFile.set(tasks.jar)
    versionName = "Veinminer Plugin - ${properties["version"]}"
    outlet.mcVersionRange = properties["paperSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    changelog = properties["changelog"] as String
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
}
