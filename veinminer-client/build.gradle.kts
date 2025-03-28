
plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core", configuration = "namedElements"))!!)
    implementation(project(":veinminer-fabric", configuration = "namedElements"))
}


sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
