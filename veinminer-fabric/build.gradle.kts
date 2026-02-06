plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core", configuration = "namedElements"))!!)
}

loom {
    accessWidenerPath = file("src/main/resources/veinminer.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
