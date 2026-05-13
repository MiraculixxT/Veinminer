package de.miraculixx.veinminer.command

object Permissions {
    @Volatile
    private var checker: (Any?, String) -> Boolean = { _, _ -> true }

    /**
     * Platform specific implementation to check for permissions
     */
    fun install(checker: (Any?, String) -> Boolean) {
        this.checker = checker
    }

    /**
     * Redirect permission check to the correct platform implementation
     */
    fun check(source: Any?, node: String): Boolean = checker(source, node)
}
