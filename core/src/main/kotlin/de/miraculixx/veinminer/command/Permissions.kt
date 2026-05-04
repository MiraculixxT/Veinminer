package de.miraculixx.veinminer.command

object Permissions {
    @Volatile
    private var checker: (Any?, String) -> Boolean = { _, _ -> true }

    fun install(checker: (Any?, String) -> Boolean) {
        this.checker = checker
    }

    fun check(source: Any?, node: String): Boolean = checker(source, node)
}
