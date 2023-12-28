plugins {
    id("com.modrinth.minotaur")
}

modrinth {
    token.set(properties["modrinthToken"] as String)
    projectId.set(properties["modrinthProjectId"] as? String ?: properties["name"] as String)
    versionNumber.set(version as String)
    versionType.set(properties["publishState"] as String)

    // Project sync
    syncBodyFrom = rootProject.file("README.md").readText()
}