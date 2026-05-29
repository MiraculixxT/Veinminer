import de.miraculixx.gradle.CurseForgePublishExtension
import dex.plugins.outlet.v2.util.ReleaseType
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.gradle.api.GradleException

plugins {
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("io.github.dexman545.outlet")
}

outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)

val curseForgePublish = extensions.create<CurseForgePublishExtension>("curseForgePublish")
curseForgePublish.versionName.convention(provider { "$name - $version" })
curseForgePublish.changelog.convention(providers.gradleProperty("changelog").orElse(""))
curseForgePublish.gameVersions.convention(emptyList())
curseForgePublish.loaders.convention(emptyList())
curseForgePublish.environments.convention(listOf("Client", "Server"))
curseForgePublish.requiredDependencies.convention(emptyList())
curseForgePublish.optionalDependencies.convention(emptyList())

modrinth {
    token.set(properties["modrinthToken"] as? String ?: "")
    projectId.set(properties["modrinthProjectId"] as? String ?: properties["name"] as String)
    versionNumber.set(version as String)
    versionType.set(properties["publishState"] as String)
    changelog.set(properties["changelog"] as String)

    // Project sync
    syncBodyFrom = rootProject.file("README.md").readText()
}

afterEvaluate {
    if (curseForgePublish.projectId.orNull.isNullOrBlank()) {
        return@afterEvaluate
    }

    tasks.register<TaskPublishCurseForge>("curseforge") {
        group = "publishing"
        description = "Publishes this project's configured artifact to CurseForge."

        doFirst {
            if ((properties["curseforgeToken"] as? String).isNullOrBlank()) {
                throw GradleException("Missing CurseForge token. Set 'curseforgeToken' in gradle.properties or pass -PcurseforgeToken=...")
            }
        }

        apiToken = properties["curseforgeToken"] as? String ?: ""
        debugMode = (properties["curseforgeDebug"] as? String)?.toBoolean() ?: false

        val artifactTask = tasks.named(curseForgePublish.artifactTaskName.get()).get()
        dependsOn(artifactTask)

        val mainFile = upload(curseForgePublish.projectId.get(), artifactTask)
        mainFile.displayName = curseForgePublish.versionName.get()
        mainFile.changelog = curseForgePublish.changelog.get()
        mainFile.changelogType = "markdown"
        mainFile.releaseType = properties["publishState"] as String

        curseForgePublish.gameVersions.get().forEach(mainFile::addGameVersion)
        curseForgePublish.loaders.get().forEach(mainFile::addModLoader)
        curseForgePublish.environments.get().forEach(mainFile::addEnvironment)
        curseForgePublish.requiredDependencies.get().forEach(mainFile::addRequirement)
        curseForgePublish.optionalDependencies.get().forEach(mainFile::addOptional)
    }
}
