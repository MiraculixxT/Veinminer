@file:Suppress("unused")

package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.mcCoroutineSync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.pattern.BlockAwareness
import de.miraculixx.veinminer.pattern.NormalStrategy
import de.miraculixx.veinminer.pattern.VeinmineAction
import de.miraculixx.veinminer.pattern.Veinmining
import de.miraculixx.veinminer.pattern.isMatureAgeTarget
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import de.miraculixx.veinminer.utils.toNMS
import de.miraculixx.veinminer.utils.toVeinminer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Loaders must populate [EventState] during init and register their own
 * block-break / attack-block hooks that delegate to [allowedToVeinmine] + [veinmine].
 *
 * Paper brings own implementation
 */
object VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()
    private val speedModifierId: Identifier = Identifier.fromNamespaceAndPath("veinminer", "veinmine_speed")

    private fun Identifier.groupedBlocks(): FixedBlockGroup<Identifier> {
        val blocks = mutableSetOf<Identifier>()
        val tools = mutableSetOf<Identifier>()
        var override: VeinminerSettingsOverride? = null

        EventState.configManager.groups.forEach {
            if (it.blocks.contains(this)) {
                if (override == null) override = it.override
                blocks.addAll(it.blocks)
                tools.addAll(it.tools)
            }
        }
        return FixedBlockGroup(blocks.toSet(), tools.toSet(), override)
    }

    fun BlockState.key(): Identifier = block.key()
    fun Block.key(): Identifier = BuiltInRegistries.BLOCK.getKey(this)
    fun ItemStack.key(): Identifier = BuiltInRegistries.ITEM.getKey(item)

    /**
     * Apply the per-block-break attribute speed modifier when a vein chain is
     * detected on attack. Returns true if a modifier was applied (caller may
     * inspect to decide further behavior).
     */
    fun applySpeedModifierOnAttack(world: Level, player: Player, pos: BlockPos, state: BlockState) {
        if (world.isClientSide || VeinminerBreakContext.isSelfInflicted) return
        player.removeMiningSpeedModifier()

        val info = allowedToVeinmine(world, player, pos, state) ?: return
        val multiplicator = info.settings.miningSpeedModifier
        if (multiplicator <= 0.0) return

        val amount = info.veinmine(false)
        if (amount <= 1) return

        val modifier = info.settings.calculateBreakSpeedModifier(amount, multiplicator)
        val attribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED) ?: return
        attribute.removeModifier(speedModifierId)
        attribute.addTransientModifier(AttributeModifier(speedModifierId, modifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
    }

    /**
     * Hook for the loader's block-break-before event. Returns true if the break
     * should proceed, false to cancel.
     */
    fun onBlockBreakBefore(world: Level, player: Player, pos: BlockPos, state: BlockState): Boolean {
        if (VeinminerBreakContext.isSelfInflicted) return true // Ignore our own events
        player.removeMiningSpeedModifier()
        val info = allowedToVeinmine(world, player, pos, state) ?: return true

        info.veinmine(true)

        val cooldownTime = info.settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uuid)
            mcCoroutineAsync(cooldownTime.ticks) { cooldown.remove(player.uuid) }
        }
        return true
    }

    /**
     * @return the veinmine action if the player is allowed to veinmine the block, null otherwise
     */
    fun allowedToVeinmine(world: Level, player: Player, pos: BlockPos, state: BlockState): VeinmineAction<ItemStack, Player>? {
        if (!ActiveHost.host.active) return null

        if (player.gameMode() == GameType.CREATIVE) return null

        val uuid = player.uuid
        val hasClient = NetworkRouter.registeredPlayers.contains(uuid)
        val material = state.key().takeIf { !state.isAir } ?: return null
        if (!state.isMatureAgeTarget()) return null

        if (hasClient && !NetworkRouter.isReady(uuid)) return null

        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val settings = EventState.configManager.settings.applyOverrides(hasClient, blockGroup.override)

        if (settings.permissionRestricted && !EventState.checkPermission(player, permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && NetworkRouter.registeredPlayers.containsKey(player.uuid)
        val isWhitelisted = isGroupBlock || EventState.configManager.veinBlocks.contains(material)

        if (settings.client.require && !hasClient) return null
        if (!isWhitelisted && !hasClientBypass) return null
        if (settings.mustSneak && !player.isCrouching) return null
        if (cooldown.contains(player.uuid)) return null

        val mainHandItem = player.mainHandItem
        val emptyHand = mainHandItem.isEmpty
        if (emptyHand && settings.needCorrectTool && !blockGroup.tools.contains(mainHandItem.key())) return null
        if (!emptyHand && settings.needCorrectTool && (state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state))) return null
        if (!hasClientBypass && isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(mainHandItem.key())) return null
        if (settings.decreaseDurability && mainHandItem.remainingDurability() <= 1) return null

        if (EventState.enchantmentActive && !mainHandItem.enchantments.keySet().any { it.`is`(EventState.enchantmentKey) }) return null

        val blocks = if (isGroupBlock && !settings.separateGroupMining) blockGroup.blocks else setOf(material)
        val face = NetworkRouter.lastSurface[player.uuid] ?: Surface.UP
        val sourceLocation = pos.toVeinminer()
        return VeinmineAction(sourceLocation, blocks, mainHandItem, player, sourceLocation, settings, face)
    }

    /**
     * Recursively break blocks around the source block until the vein stops.
     * @return the number of blocks broken
     */
    fun VeinmineAction<ItemStack, Player>.veinmine(shouldBreak: Boolean): Int {
        val iPlayer = player
        val world = iPlayer.level()

        val strategy = NetworkRouter.activeStrategy(iPlayer.uuid) ?: NormalStrategy
        val maxDepth = NetworkRouter.maxDepth(iPlayer.uuid)

        val blockAwareness = object : BlockAwareness {
            override fun getBlockType(pos: BlockPosition): Identifier {
                return world.getBlockState(BlockPos(pos.x, pos.y, pos.z)).key()
            }

            override fun isActionTarget(pos: BlockPosition): Boolean {
                return world.getBlockState(BlockPos(pos.x, pos.y, pos.z)).isMatureAgeTarget()
            }

            override fun breakBlock(pos: BlockPosition, ticks: Int): Boolean {
                if (!shouldBreak) return false // safeguard
                if (tool.remainingDurability() <= 1) return false // tool "broken"
                if (settings.hungerPerBlock > 0.0 && iPlayer.foodData.foodLevel == 0 && iPlayer.foodData.saturationLevel == 0f) return false // hunger depleted
                val blockPos = BlockPos(pos.x, pos.y, pos.z)
                scheduleBreak(blockPos, ticks)
                return true
            }
        }

        val hits = Veinmining.veinmine(this, blockAwareness, strategy, maxDepth, shouldBreak)
        return hits.size
    }

    private fun VeinmineAction<ItemStack, Player>.scheduleBreak(
        pos: BlockPos,
        delay: Int
    ) {
        mcCoroutineSync(mcServer!!, delay) {
            val iPlayer = player as? ServerPlayer ?: return@mcCoroutineSync
            val world = iPlayer.level()
            val state = world.getBlockState(pos)
            if (!targetTypes.contains(state.key())) return@mcCoroutineSync
            if (!state.isMatureAgeTarget()) return@mcCoroutineSync
            val iTool = iPlayer.mainHandItem
            if (settings.decreaseDurability && iTool.remainingDurability() <= 1) return@mcCoroutineSync

            // Vanilla always damages the tool. Zero the damage across the call so it cannot break, then restore
            val protectTool = !settings.decreaseDurability && !iTool.isEmpty && iTool.maxDamage > 0
            val damageBefore = iTool.damageValue
            if (protectTool) iTool.damageValue = 0

            val dropTarget = if (settings.mergeItemDrops) Vec3.atCenterOf(sourceLocation.toNMS()) else null
            val broken = VeinminerBreakContext.breaking(dropTarget) { iPlayer.gameMode.destroyBlock(pos) }

            if (protectTool && !iTool.isEmpty) iTool.damageValue = damageBefore
            if (!broken) return@mcCoroutineSync

            // Emits block break effect
            if (state.block is BaseFireBlock) world.levelEvent(LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0)
            else world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state))

            if (settings.hungerPerBlock > 0.0) iPlayer.causeFoodExhaustion(settings.hungerPerBlock.toFloat())
        }
    }

    private fun ItemStack.remainingDurability(): Int {
        if (isEmpty) return Int.MAX_VALUE
        if (maxDamage <= 0) return Int.MAX_VALUE
        return maxDamage - damageValue
    }

    fun Player.removeMiningSpeedModifier() {
        getAttribute(Attributes.BLOCK_BREAK_SPEED)?.removeModifier(speedModifierId)
    }
}
