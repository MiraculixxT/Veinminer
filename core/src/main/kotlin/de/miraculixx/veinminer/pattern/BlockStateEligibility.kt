package de.miraculixx.veinminer.pattern

import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty

private const val AGE_PROPERTY = "age"

fun BlockState.isMatureAgeTarget(): Boolean {
    val ageProperty = getAgeProperty() ?: return true
    val maxAge = ageProperty.possibleValues.maxOrNull() ?: return true
    return getValue(ageProperty) == maxAge
}

private fun BlockState.getAgeProperty(): IntegerProperty? {
    return properties.asSequence()
        .filterIsInstance<IntegerProperty>()
        .firstOrNull { it.name == AGE_PROPERTY }
}
