plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-fabric"))
}

loom {
    accessWidenerPath = file("src/main/resources/veinminerClient.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}
