package de.miraculixx.veinminer.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

@DslMarker
annotation class VeinminerCommandDsl

@VeinminerCommandDsl
class CommandNodeBuilder<S>(val builder: ArgumentBuilder<S, *>) {

    fun literal(name: String, block: CommandNodeBuilder<S>.() -> Unit) {
        val child = LiteralArgumentBuilder.literal<S>(name)
        CommandNodeBuilder(child).apply(block)
        builder.then(child)
    }

    fun <T> argument(name: String, type: ArgumentType<T>, block: CommandNodeBuilder<S>.() -> Unit) {
        val child = RequiredArgumentBuilder.argument<S, T>(name, type)
        CommandNodeBuilder(child).apply(block)
        builder.then(child)
    }

    fun intArg(
        name: String,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        block: CommandNodeBuilder<S>.() -> Unit,
    ) = argument(name, IntegerArgumentType.integer(min, max), block)

    fun doubleArg(
        name: String,
        min: Double = -Double.MAX_VALUE,
        max: Double = Double.MAX_VALUE,
        block: CommandNodeBuilder<S>.() -> Unit,
    ) = argument(name, DoubleArgumentType.doubleArg(min, max), block)

    fun stringArg(name: String, block: CommandNodeBuilder<S>.() -> Unit) =
        argument(name, StringArgumentType.string(), block)

    fun greedyStringArg(name: String, block: CommandNodeBuilder<S>.() -> Unit) =
        argument(name, StringArgumentType.greedyString(), block)

    fun boolArg(name: String, block: CommandNodeBuilder<S>.() -> Unit) =
        argument(name, BoolArgumentType.bool(), block)

    fun requires(predicate: (S) -> Boolean) {
        builder.requires(predicate)
    }

    fun requiresPermission(node: String) {
        builder.requires { src -> Permissions.check(src as Any?, node) }
    }

    fun executes(block: CommandContext<S>.() -> Unit) {
        builder.executes(Command<S> { ctx ->
            try {
                block(ctx)
                Command.SINGLE_SUCCESS
            } catch (t: Throwable) {
                t.printStackTrace()
                0
            }
        })
    }

    fun executesAsync(block: CommandContext<S>.() -> Unit) {
        builder.executes(Command<S> { ctx ->
            CompletableFuture.runAsync {
                try {
                    block(ctx)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            Command.SINGLE_SUCCESS
        })
    }

    fun suggests(provider: (CommandContext<S>, SuggestionsBuilder) -> CompletableFuture<Suggestions>) {
        val req = builder as? RequiredArgumentBuilder<S, *>
            ?: error("suggests() may only be called inside an argument {} block")
        req.suggests(SuggestionProvider { ctx, sb -> provider(ctx, sb) })
    }

    fun suggestStrings(provider: (CommandContext<S>) -> Iterable<String>) {
        suggests { ctx, sb ->
            val remaining = sb.remaining.lowercase()
            for (s in provider(ctx)) {
                if (remaining.isEmpty() || s.lowercase().startsWith(remaining)) sb.suggest(s)
            }
            sb.buildFuture()
        }
    }
}

fun <S> veinminerCommand(name: String, block: CommandNodeBuilder<S>.() -> Unit): LiteralArgumentBuilder<S> {
    val root = LiteralArgumentBuilder.literal<S>(name)
    CommandNodeBuilder(root).apply(block)
    return root
}
