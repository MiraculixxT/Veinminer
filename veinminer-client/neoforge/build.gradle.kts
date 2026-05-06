plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}


dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-neoforge"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
