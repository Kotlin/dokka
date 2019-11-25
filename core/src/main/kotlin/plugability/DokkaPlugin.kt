package org.jetbrains.dokka.plugability

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance

private typealias ExtensionDelegate<T> = ReadOnlyProperty<DokkaPlugin, Extension<T>>

abstract class DokkaPlugin {
    private val extensionDelegates = mutableListOf<KProperty<*>>()

    @PublishedApi
    internal var context: DokkaContext? = null

    protected inline fun <reified T : DokkaPlugin> plugin(): T =
        context?.plugin(T::class) ?: T::class.createInstance().also { it.context = this.context }

    protected fun <T : Any> extensionPoint() =
        object : ReadOnlyProperty<DokkaPlugin, ExtensionPoint<T>> {
            override fun getValue(thisRef: DokkaPlugin, property: KProperty<*>) = ExtensionPoint<T>(
                thisRef::class.qualifiedName ?: throw AssertionError("Plugin must be named class"),
                property.name
            )
        }

    protected fun <T: Any> extending(definition: ExtendingDSL.() -> Extension<T>) = ExtensionProvider(definition)

    protected class ExtensionProvider<T: Any> internal constructor(
        private val definition: ExtendingDSL.() -> Extension<T>
    ) {
        operator fun provideDelegate(thisRef: DokkaPlugin, property: KProperty<*>) = lazy {
            ExtendingDSL(
                thisRef::class.qualifiedName ?: throw AssertionError("Plugin must be named class"),
                property.name
            ).definition()
        }.also { thisRef.extensionDelegates += property }
    }

    internal fun internalInstall(ctx: DokkaContextConfiguration) {
        extensionDelegates.asSequence()
            .filterIsInstance<KProperty1<DokkaPlugin, Extension<*>>>() // always true
            .map { it.get(this) }
            .forEach { ctx.addExtension(it) }
    }


}