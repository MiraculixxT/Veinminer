repositories {
    mavenCentral()
}

allprojects {
    group = properties["group"] as String
    version = properties["version"] as String
}