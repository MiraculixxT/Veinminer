package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import net.minecraft.client.KeyMapping
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent

object NeoForgeKeyBindings {
    private const val CATEGORY = "key.category.$MOD_ID.keybinds"

    fun register(event: RegisterKeyMappingsEvent) {
        val h = KeyMapping("key.$MOD_ID.hold", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, CATEGORY)
        val t = KeyMapping("key.$MOD_ID.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.value, CATEGORY)
        val c = KeyMapping("key.$MOD_ID.config", InputConstants.Type.KEYSYM, InputConstants.KEY_O, CATEGORY)
        event.register(h)
        event.register(t)
        event.register(c)
        KeyBindings.hold = h
        KeyBindings.toggle = t
        KeyBindings.config = c
    }
}
