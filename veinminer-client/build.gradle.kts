import dex.plugins.outlet.v2.util.ReleaseType

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
