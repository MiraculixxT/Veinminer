package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent

object NeoForgeKeyBindings {
    private val category = KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "keybinds"))

    fun register(event: RegisterKeyMappingsEvent) {
        event.registerCategory(category)
        val h = KeyMapping("key.$MOD_ID.hold", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, category)
        val t = KeyMapping("key.$MOD_ID.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.value, category)
        event.register(h)
        event.register(t)
        KeyBindings.hold = h
        KeyBindings.toggle = t
    }
}
