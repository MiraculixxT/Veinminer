plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":veinminer:veinminer-common"))
    implementation(project(":veinminer:veinminer-neoforge"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer-client/assets/")
    }
}
