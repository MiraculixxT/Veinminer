
plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core"))!!)
}


sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
