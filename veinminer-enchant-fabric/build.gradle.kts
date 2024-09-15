import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {

}

modrinth {
    uploadFile.set(tasks.remapJar)
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
    })
    dependencies {
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
        required.project("fabric-api")
        required.project("fabric-language-kotlin")

        val useConfig = properties["useConfig"] as String == "true"
        if (useConfig) {
            optional.project("cloth-config")
        }
    }
}
