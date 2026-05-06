plugins {
    `kotlin-script`
    `neoforge-script`
    `publish-script`
}

neoForge {
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}

dependencies {
    implementation(project(":core"))
    implementation(project(":veinminer:veinminer-common"))

    jarJar(project(":core"))
    jarJar(project(":veinminer:veinminer-common"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/veinminer/assets/")
    }
}
