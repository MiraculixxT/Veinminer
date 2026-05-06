package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminerClient.VeinminerClient.Companion.MOD_ID
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent

object KeyBindings {
    private val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keybinds"))

    var hold: KeyMapping? = null
        private set
    var toggle: KeyMapping? = null
        private set

    fun register(event: RegisterKeyMappingsEvent) {
        val h = KeyMapping("key.$MOD_ID.hold", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, category)
        val t = KeyMapping("key.$MOD_ID.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.value, category)
        event.register(h)
        event.register(t)
        hold = h
        toggle = t
    }
}
