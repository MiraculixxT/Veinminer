plugins {
    id("com.modrinth.minotaur")
    id("io.github.dexman545.outlet")
}

modrinth {
    token.set(properties["modrinthToken"] as? String ?: "")
    projectId.set(properties["modrinthProjectId"] as? String ?: properties["name"] as String)
    versionNumber.set(version as String)
    versionType.set(properties["publishState"] as String)
    changelog.set(properties["changelog"] as String)

    // Project sync
    syncBodyFrom = rootProject.file("README.md").readText()
}
