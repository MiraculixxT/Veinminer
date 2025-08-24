plugins {
    base
}

val packs = listOf(
    Datapack("veinminer", "1.3.2", "veinminer"),
    Datapack("enchantment", "1.2.5", "veinminer-enchantment")
)

// Create a zip task for each datapack
packs.forEach { pack ->
    tasks.register<Zip>("zip${pack.name.replaceFirstChar { it.uppercase() }}") {
        group = "zip"
        description = "Zips the contents of '${pack.target}' into '${pack.name}-${pack.version}.zip'"

        from(file(pack.target)) {
            into("") // Include contents only, not the folder itself
        }

        archiveFileName.set("${pack.name}-${pack.version}.zip")
        destinationDirectory.set(layout.buildDirectory)

        doLast {
            println("Created zip: ${destinationDirectory.get().asFile}/${archiveFileName.get()}")
        }
    }
}

// Combine all zips into a single task
tasks.register("zipAll") {
    group = "zip"
    description = "Zips all datapacks"

    dependsOn(packs.map { "zip${it.name.replaceFirstChar { c -> c.uppercase() }}" })
}

data class Datapack(
    val target: String,
    val version: String,
    val name: String
)
