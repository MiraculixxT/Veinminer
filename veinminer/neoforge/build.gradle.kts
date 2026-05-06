plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}


dependencies {
    implementation(project(":core"))
    implementation(project(":veinminer:veinminer-common"))

    jarJar(project(":core"))
    jarJar(project(":veinminer:veinminer-common"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
