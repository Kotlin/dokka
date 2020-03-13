package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

data class ExtensionPoint<T : Any> internal constructor(
    internal val pluginClass: String,
    internal val pointName: String
) {
    override fun toString() = "ExtensionPoint: $pluginClass/$pointName"
}

abstract class Extension<T : Any> internal constructor(
    internal val extensionPoint: ExtensionPoint<T>,
    internal val pluginClass: String,
    internal val extensionName: String,
    internal val action: LazyEvaluated<T>,
    internal val ordering: (OrderDsl.() -> Unit)? = null,
    internal val condition: DokkaConfiguration.() -> Boolean = { true },
    internal val isFallback: Boolean
) {
    override fun toString() = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?) =
        if (other is Extension<*>) this.pluginClass == other.pluginClass && this.extensionName == other.extensionName
        else false

    override fun hashCode() = listOf(pluginClass, extensionName).hashCode()

    abstract fun setCondition(condition: (DokkaConfiguration.() -> Boolean)): Extension<T>

    abstract fun markedAsFallback(): Extension<T>
}

class ExtensionOrdered<T : Any> internal constructor(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>,
    ordering: (OrderDsl.() -> Unit),
    condition: DokkaConfiguration.() -> Boolean = { true },
    isFallback: Boolean = false
) : Extension<T>(
    extensionPoint,
    pluginClass,
    extensionName,
    action,
    ordering,
    condition,
    isFallback
) {
    override fun setCondition(condition: DokkaConfiguration.() -> Boolean) =
        ExtensionOrdered(extensionPoint, pluginClass, extensionName, action, ordering!!, condition)

    override fun markedAsFallback() =
        ExtensionOrdered(extensionPoint, pluginClass, extensionName, action, ordering!!, condition, true)
}

class ExtensionUnordered<T : Any> internal constructor(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>,
    condition: DokkaConfiguration.() -> Boolean = { true },
    isFallback: Boolean = false
) : Extension<T>(
    extensionPoint,
    pluginClass,
    extensionName,
    action,
    null,
    condition,
    isFallback
) {
    override fun setCondition(condition: DokkaConfiguration.() -> Boolean) =
        ExtensionUnordered(extensionPoint, pluginClass, extensionName, action, condition)

    override fun markedAsFallback() =
        ExtensionUnordered(extensionPoint, pluginClass, extensionName, action, condition, true)
}

internal data class Ordering(val previous: Set<Extension<*>>, val following: Set<Extension<*>>)

@DslMarker
annotation class ExtensionsDsl

@ExtensionsDsl
class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {

    infix fun <T : Any> ExtensionPoint<T>.with(action: T) =
        ExtensionUnordered(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromInstance(action))

    infix fun <T : Any> ExtensionPoint<T>.providing(action: (DokkaContext) -> T) =
        ExtensionUnordered(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromRecipe(action))

    infix fun <T : Any> ExtensionUnordered<T>.order(block: OrderDsl.() -> Unit) =
        ExtensionOrdered(extensionPoint, pluginClass, extensionName, action, block)

    infix fun <T : Any> Extension<T>.applyIf(condition: DokkaConfiguration.() -> Boolean): Extension<T> =
        this.setCondition { this@applyIf.condition(this) && condition(this) }

}

@ExtensionsDsl
class OrderDsl {
    internal val previous = mutableSetOf<Extension<*>>()
    internal val following = mutableSetOf<Extension<*>>()

    fun after(vararg extensions: Extension<*>) {
        previous += extensions
    }

    fun before(vararg extensions: Extension<*>) {
        following += extensions
    }
}