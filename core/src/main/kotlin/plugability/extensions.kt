package org.jetbrains.dokka.plugability

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
    internal val ordering: (OrderDsl.() -> Unit)? = null
) {
    override fun toString() = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?) =
        if (other is Extension<*>) this.pluginClass == other.pluginClass && this.extensionName == other.extensionName
        else false

    override fun hashCode() = listOf(pluginClass, extensionName).hashCode()
}

class ExtensionOrdered<T : Any> internal constructor(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>,
    ordering: (OrderDsl.() -> Unit)
) : Extension<T>(
    extensionPoint,
    pluginClass,
    extensionName,
    action,
    ordering
)

class ExtensionUnordered<T : Any> internal constructor(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>
) : Extension<T>(
    extensionPoint,
    pluginClass,
    extensionName,
    action,
    null
)

internal data class Ordering(val previous: Set<Extension<*>>, val following: Set<Extension<*>>)

@DslMarker
annotation class ExtensionsDsl

@ExtensionsDsl
class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {

    infix fun <T : Any> ExtensionPoint<T>.with(action: T) =
        ExtensionUnordered(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromInstance(action))

    infix fun <T : Any> ExtensionPoint<T>.providing(action: (DokkaContext) -> T) =
        ExtensionUnordered(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromRecipe(action))

    infix fun <T: Any> ExtensionUnordered<T>.order(block: OrderDsl.() -> Unit) =
        ExtensionOrdered(extensionPoint, pluginClass, extensionName, action, block)
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