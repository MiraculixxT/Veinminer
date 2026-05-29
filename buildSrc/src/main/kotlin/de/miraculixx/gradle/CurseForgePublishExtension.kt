package de.miraculixx.gradle

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CurseForgePublishExtension @Inject constructor(objects: ObjectFactory) {
    val projectId: Property<String> = objects.property(String::class.java).convention("")
    val artifactTaskName: Property<String> = objects.property(String::class.java).convention("jar")
    val versionName: Property<String> = objects.property(String::class.java)
    val changelog: Property<String> = objects.property(String::class.java)
    val gameVersions: ListProperty<String> = objects.listProperty(String::class.java)
    val loaders: ListProperty<String> = objects.listProperty(String::class.java)
    val environments: ListProperty<String> = objects.listProperty(String::class.java)
    val requiredDependencies: ListProperty<String> = objects.listProperty(String::class.java)
    val optionalDependencies: ListProperty<String> = objects.listProperty(String::class.java)
}

fun Project.curseForgePublish(action: CurseForgePublishExtension.() -> Unit) {
    extensions.configure(CurseForgePublishExtension::class.java, action)
}
