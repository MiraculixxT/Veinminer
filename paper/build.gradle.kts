import dex.plugins.outlet.v2.util.ReleaseType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

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
}

tasks.jar {
    archivesName = "${properties["projectName"]}"
}
