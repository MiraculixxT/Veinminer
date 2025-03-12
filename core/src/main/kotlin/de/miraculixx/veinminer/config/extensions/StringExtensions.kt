package de.miraculixx.veinminer.config.extensions

fun String.fancy(): String {
        val split = split('_') //GRASS_BLOCK -> [GRASS, BLOCK]
        return buildString {
            split.forEach { word ->
                append(word[0].uppercase() + word.substring(1).lowercase() + " ") //GRASS -> Grass
            }
        }.removeSuffix(" ")
    }
