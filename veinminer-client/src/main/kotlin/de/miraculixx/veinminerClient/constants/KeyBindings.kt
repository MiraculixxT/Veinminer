package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminerClient.VeinminerClient.Companion.MOD_ID
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

val KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keybinds"))
val KEY_VEINMINE_HOLD = registerKeyBinding("hold", InputConstants.KEY_Y)
val KEY_VEINMINE_TOGGLE = registerKeyBinding("toggle", InputConstants.UNKNOWN.value)

private fun registerKeyBinding(id: String, key: Int) = KeyBindingHelper.registerKeyBinding(
    KeyMapping(
        "key.$MOD_ID.$id",
        InputConstants.Type.KEYSYM,
        key,
        KEY_CATEGORY
    )
)
