package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping

object FabricKeyBindings {
    private const val CATEGORY = "key.category.$MOD_ID.keybinds"

    fun register() {
        KeyBindings.hold = registerKeyBinding("hold", InputConstants.KEY_Y)
        KeyBindings.toggle = registerKeyBinding("toggle", InputConstants.UNKNOWN.value)
        KeyBindings.config = registerKeyBinding("config", InputConstants.KEY_O)
    }

    private fun registerKeyBinding(id: String, key: Int) = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.$MOD_ID.$id",
            InputConstants.Type.KEYSYM,
            key,
            CATEGORY
        )
    )
}
