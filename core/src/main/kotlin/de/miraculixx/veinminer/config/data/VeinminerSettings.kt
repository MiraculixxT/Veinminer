package de.miraculixx.veinminer.config.data

import kotlinx.serialization.Serializable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

@Serializable
data class VeinminerSettings(
    var cooldown: Int = 20,
    var mustSneak: Boolean = false,
    var delay: Int = 0,
    var maxChain: Int = 100,
    var needCorrectTool: Boolean = true,
    var searchRadius: Int = 1,
    var permissionRestricted: Boolean = false,
    var mergeItemDrops: Boolean = false,
    var autoUpdate: Boolean = false,
    var decreaseDurability: Boolean = true,
    var miningSpeedModifier: Double = 0.0,
    val client: VeinminerClientSettings = VeinminerClientSettings()
) {
    /**
     * Apply all possible overrides to settings.
     * Priority: default < client < group
     * @param isClient If the user has the client mod installed
     * @param g Optional group override
     */
    fun applyOverrides(isClient: Boolean, g: VeinminerSettingsOverride?): VeinminerSettings {
        if (!isClient && g == null) return this
        val c = client.overrides

        return VeinminerSettings(
            cooldown = g?.cooldown ?: (if (isClient) c.cooldown ?: cooldown else cooldown),
            mustSneak = g?.mustSneak ?: (if (isClient) c.mustSneak ?: mustSneak else mustSneak),
            delay = g?.delay ?: (if (isClient) c.delay ?: delay else delay),
            maxChain = g?.maxChain ?: (if (isClient) c.maxChain ?: maxChain else maxChain),
            needCorrectTool = g?.needCorrectTool ?: (if (isClient) c.needCorrectTool ?: needCorrectTool else needCorrectTool),
            searchRadius = g?.searchRadius ?: (if (isClient) c.searchRadius ?: searchRadius else searchRadius),
            permissionRestricted = g?.permissionRestricted ?: (if (isClient) c.permissionRestricted ?: permissionRestricted else permissionRestricted),
            mergeItemDrops = mergeItemDrops,
            decreaseDurability = g?.decreaseDurability ?: (if (isClient) c.decreaseDurability ?: decreaseDurability else decreaseDurability),
            miningSpeedModifier = g?.miningSpeedModifier ?: (if (isClient) c.miningSpeedModifier ?: miningSpeedModifier else miningSpeedModifier),
            client = client
        )
    }

    /**
     * Calculate the break speed modifier based on vein size and a multiplicator.
     * Used for Operation.MULTIPLY_SCALAR_1
     */
    fun calculateBreakSpeedModifier(
        veinSize: Int,
        multiplicator: Double
    ): Double {
        val n = veinSize.coerceAtLeast(1)
        val m = multiplicator.coerceIn(0.0, 1.0)

        val timeFactor = 1.0 + m * (n - 1) // 0->1x, 1->nx time
        val speedFactor = (1.0 / timeFactor).coerceAtLeast(0.01) // 100x max decrease

        return speedFactor - 1.0
    }
}

@Serializable
data class VeinminerClientSettings(
    var allow: Boolean = true,
    var require: Boolean = false,
    var translucentBlockHighlight: Boolean = true,
    var allBlocks: Boolean = false,
    val overrides: VeinminerSettingsOverride = VeinminerSettingsOverride()
)

@Serializable
data class VeinminerSettingsOverride(
    var cooldown: Int? = null,
    var mustSneak: Boolean? = null,
    var delay: Int? = null,
    var maxChain: Int? = null,
    var needCorrectTool: Boolean? = null,
    var searchRadius: Int? = null,
    var permissionRestricted: Boolean? = null,
    var decreaseDurability: Boolean? = null,
    var miningSpeedModifier: Double? = null
) {
    fun nonNullKeys(): List<String> =
        VeinminerSettingsOverride::class.memberProperties.mapNotNull { prop -> prop.get(this)?.let { prop.name } }

    fun unset(key: String): Boolean {
        val prop = VeinminerSettingsOverride::class.memberProperties
            .find { it.name == key } as? KMutableProperty1<VeinminerSettingsOverride, Any?>
            ?: return false
        prop.setter.call(this, null)
        return true
    }
}
