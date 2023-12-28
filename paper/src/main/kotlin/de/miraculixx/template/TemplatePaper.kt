package de.miraculixx.template

import de.miraculixx.kpaper.main.KPaper

class TemplatePaper: KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
    }

    override fun load() {
        INSTANCE = this
    }

    override fun startup() {

    }

    override fun shutdown() {

    }
}

val INSTANCE by lazy { TemplatePaper.INSTANCE }