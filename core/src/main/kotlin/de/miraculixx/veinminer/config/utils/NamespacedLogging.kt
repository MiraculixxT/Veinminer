package de.miraculixx.veinminer.config.utils

import com.mojang.logging.LogUtils
import org.slf4j.Logger

class NamespacedLogging(
    val namespace: String
) {
    val logger: Logger = LogUtils.getLogger()

    fun info(message: String) {
        logger.info("[$namespace] $message")
    }

    fun warn(message: String) {
        logger.warn("[$namespace] $message")
    }

    fun error(message: String) {
        logger.error("[$namespace] $message")
    }
}