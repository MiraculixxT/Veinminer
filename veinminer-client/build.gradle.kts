
plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core", configuration = "namedElements"))!!)
}


sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
