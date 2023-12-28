package de.miraculixx.template.client

import net.fabricmc.api.ClientModInitializer

class TemplateClient: ClientModInitializer {

    override fun onInitializeClient() {
        println("Hello client!")
    }

}