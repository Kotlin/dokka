package org.jetbrains.dokka.plugability

data class ExtensionPoint<T : Any> internal constructor(
    internal val pluginClass: String,
    internal val pointName: String
) {
    override fun toString() = "ExtensionPoint: $pluginClass/$pointName"
}

class Extension<T : Any> internal constructor(
    internal val extensionPoint: ExtensionPoint<T>,
    internal val pluginClass: String,
    internal val extensionName: String,
    internal val action: T,
    internal val ordering: (OrderDsl.() -> Unit)? = null
) {
    override fun toString() = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?) =
        if (other is Extension<*>) this.pluginClass == other.extensionName && this.extensionName == other.extensionName
        else false

    override fun hashCode() = listOf(pluginClass, extensionName).hashCode()
}

internal data class Ordering(val previous: Set<Extension<*>>, val following: Set<Extension<*>>)

@DslMarker
annotation class ExtensionsDsl

@ExtensionsDsl
class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {
    infix fun <T: Any> ExtensionPoint<T>.with(action: T) =
        Extension(this, this@ExtendingDSL.pluginClass, extensionName, action)

    infix fun <T: Any> Extension<T>.order(block: OrderDsl.() -> Unit) =
        Extension(extensionPoint, pluginClass, extensionName, action, block)
}

@ExtensionsDsl
class OrderDsl {
    private val previous = mutableSetOf<Extension<*>>()
    private val following = mutableSetOf<Extension<*>>()

    fun after(vararg extensions: Extension<*>) {
        previous += extensions
    }

    fun before(vararg extensions: Extension<*>) {
        following += extensions
    }
}