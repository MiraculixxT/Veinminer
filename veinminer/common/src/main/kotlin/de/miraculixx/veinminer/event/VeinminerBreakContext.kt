package de.miraculixx.veinminer.event

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

/**
 * Server-thread state shared between the veinmine break loop and the loader entity hooks
 */
object VeinminerBreakContext {
    private val selfInflicted = ThreadLocal.withInitial { false }
    private val dropTarget = ThreadLocal<Vec3?>()

    val isSelfInflicted: Boolean
        get() = selfInflicted.get()

    /** Runs [block] as a veinminer-owned break, optionally redirecting its drops to [target] */
    fun <T> breaking(target: Vec3?, block: () -> T): T {
        val previousFlag = selfInflicted.get()
        val previousTarget = dropTarget.get()
        selfInflicted.set(true)
        dropTarget.set(target)
        try {
            return block()
        } finally {
            selfInflicted.set(previousFlag)
            dropTarget.set(previousTarget)
        }
    }

    /** Redirect drops when mergeDrops setting is on */
    fun relocateDrop(entity: Entity) {
        val target = dropTarget.get() ?: return
        if (entity !is ItemEntity && entity !is ExperienceOrb) return
        entity.snapTo(target)
    }
}
