package de.miraculixx.template.server

import net.fabricmc.api.DedicatedServerModInitializer

class TemplateServer: DedicatedServerModInitializer {

    override fun onInitializeServer() {
        println("Hello server!")
    }

}