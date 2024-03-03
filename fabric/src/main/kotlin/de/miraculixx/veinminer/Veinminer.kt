package de.miraculixx.veinminer

import de.miraculixx.veinminer.command.VeinminerCommand
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import java.util.logging.Logger


object Veinminer : ModInitializer {
    const val MOD_ID = "veinminer"
    lateinit var INSTANCE: ModContainer
    lateinit var FABRIC: FabricLoader

    override fun onInitialize() {
        FABRIC = FabricLoader.getInstance()
        INSTANCE = FABRIC.getModContainer(MOD_ID).get()
        LOGGER.info("Veinminer Version: ${INSTANCE.metadata.version} (fabric)")

        VeinminerCommand
    }


}

val LOGGER: Logger = Logger.getLogger(Veinminer.MOD_ID)