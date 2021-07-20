package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

data class ExtensionPoint<T : Any> internal constructor(
    internal val pluginClass: String,
    internal val pointName: String
) {
    override fun toString() = "ExtensionPoint: $pluginClass/$pointName"
}

sealed class OrderingKind {
    object None : OrderingKind()
    class ByDsl(val block: (OrderDsl.() -> Unit)) : OrderingKind()
}

sealed class OverrideKind {
    object None : OverrideKind()
    class Present(val overriden: List<Extensionable<*, *, *>>) : OverrideKind()
}

sealed class Extensionable<T, Ordering : OrderingKind, Override : OverrideKind>

class UnregisteredExtension<T, Ordering : OrderingKind, Override : OverrideKind> : Extensionable<T, Ordering, Override>()

class Extension<T : Any, Ordering : OrderingKind, Override : OverrideKind> internal constructor(
    internal val extensionPoint: ExtensionPoint<T>,
    internal val pluginClass: String,
    internal val extensionName: String,
    internal val action: LazyEvaluated<T>,
    internal val ordering: Ordering,
    internal val override: Override,
    internal val conditions: List<DokkaConfiguration.() -> Boolean>
) : Extensionable<T, Ordering, Override>() {
    override fun toString() = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?) =
        if (other is Extension<*, *, *>) this.pluginClass == other.pluginClass && this.extensionName == other.extensionName
        else false

    override fun hashCode() = listOf(pluginClass, extensionName).hashCode()

    val condition: DokkaConfiguration.() -> Boolean
        get() = { conditions.all { it(this) } }
}

internal fun <T : Any> Extension(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>
) = Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.None, OverrideKind.None, emptyList())

@DslMarker
annotation class ExtensionsDsl

@ExtensionsDsl
class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {

    infix fun <T : Any> ExtensionPoint<T>.with(action: T): Extensionable<T, OrderingKind.None, OverrideKind.None> =
            Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromInstance(action))

    infix fun <T : Any> ExtensionPoint<T>.providing(action: (DokkaContext) -> T): Extension<T, OrderingKind.None, OverrideKind.None> =
            Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromRecipe(action))

    infix fun <T : Any, Override : OverrideKind> Extensionable<T, OrderingKind.None, Override>.order(
            block: OrderDsl.() -> Unit
    ) = this.takeIf { it is Extension<T, OrderingKind.None, Override> }?.run {
        this as Extension<T, OrderingKind.None, Override>
        Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.ByDsl(block), override, conditions)
    } ?: this

    infix fun <T : Any, Override : OverrideKind, Ordering : OrderingKind> Extensionable<T, Ordering, Override>.applyIf(
            condition: DokkaConfiguration.() -> Boolean
    ) = this.takeIf { it is Extension<T, Ordering, Override> }?.run {
        this as Extension<T, Ordering, Override>
        Extension(extensionPoint, pluginClass, extensionName, action, ordering, override, conditions + condition)
    } ?: this

    infix fun <T : Any, Override : OverrideKind, Ordering : OrderingKind> Extensionable<T, Ordering, Override>.override(
            overriden: List<Extensionable<T, *, *>>
    ): Extensionable<T, Ordering, out OverrideKind> = this.takeIf { it is Extension<T, Ordering, Override> }?.run {
        this as Extension<T, Ordering, Override>
        Extension(extensionPoint, pluginClass, extensionName, action, ordering, OverrideKind.Present(overriden), conditions)
    } ?: this

    infix fun <T : Any, Override : OverrideKind, Ordering : OrderingKind> Extensionable<T, Ordering, Override>.override(
            overriden: Extensionable<T, *, *>
    ) = override(listOf(overriden))
}

@ExtensionsDsl
class OrderDsl {
    internal val previous = mutableSetOf<Extension<*, *, *>>()
    internal val following = mutableSetOf<Extension<*, *, *>>()

    fun after(vararg extensions: Extensionable<*, *, *>) {
        previous += extensions.filterIsInstance<Extension<*, *, *>>()
    }

    fun before(vararg extensions: Extensionable<*, *, *>) {
        following += extensions.filterIsInstance<Extension<*, *, *>>()
    }
}
