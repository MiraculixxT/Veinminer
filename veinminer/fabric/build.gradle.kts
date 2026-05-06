plugins {
    `kotlin-script`
    `fabric-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":core"))!!)
    implementation(include(project(":veinminer:veinminer-common"))!!)
}

loom {
    accessWidenerPath = file("src/main/resources/veinminer.accesswidener")
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}
