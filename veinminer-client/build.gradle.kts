plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core"))!!)
    implementation(project(":veinminer-fabric", configuration = "namedElements"))
}

loom {
    accessWidenerPath = file("src/main/resources/veinminerClient.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
