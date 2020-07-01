package org.jetbrains.dokka.plugability

import com.google.gson.Gson
import org.jetbrains.dokka.DokkaConfiguration
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance

private typealias ExtensionDelegate<T> = ReadOnlyProperty<DokkaPlugin, Extension<T>>

abstract class DokkaPlugin {
    private val extensionDelegates = mutableListOf<KProperty<*>>()

    @PublishedApi
    internal var context: DokkaContext? = null

    protected inline fun <reified T : DokkaPlugin> plugin(): T = context?.plugin(T::class) ?: throwIllegalQuery()

    protected fun <T : Any> extensionPoint() =
        object : ReadOnlyProperty<DokkaPlugin, ExtensionPoint<T>> {
            override fun getValue(thisRef: DokkaPlugin, property: KProperty<*>) = ExtensionPoint<T>(
                thisRef::class.qualifiedName ?: throw AssertionError("Plugin must be named class"),
                property.name
            )
        }

    protected fun <T : Any> extending(isFallback: Boolean = false, definition: ExtendingDSL.() -> Extension<T>) =
        if (isFallback) {
            ExtensionProvider { definition().markedAsFallback() }
        } else {
            ExtensionProvider(definition)
        }

    protected class ExtensionProvider<T : Any> internal constructor(
        private val definition: ExtendingDSL.() -> Extension<T>
    ) {
        operator fun provideDelegate(thisRef: DokkaPlugin, property: KProperty<*>) = lazy {
            ExtendingDSL(
                thisRef::class.qualifiedName ?: throw AssertionError("Plugin must be named class"),
                property.name
            ).definition()
        }.also { thisRef.extensionDelegates += property }
    }

    internal fun internalInstall(ctx: DokkaContextConfiguration, configuration: DokkaConfiguration) {
        extensionDelegates.asSequence()
            .filterIsInstance<KProperty1<DokkaPlugin, Extension<*>>>() // should be always true
            .map { it.get(this) }
            .forEach { if (it.condition.invoke(configuration)) ctx.addExtensionDependencies(it) }
    }
}

interface Configurable {
    val pluginsConfiguration: Map<String, String>
}

interface ConfigurableBlock

inline fun <reified P : DokkaPlugin, reified T : ConfigurableBlock> Configurable.pluginConfiguration(block: T.() -> Unit) {
    val instance = T::class.createInstance().apply(block)

    val mutablePluginsConfiguration = pluginsConfiguration as MutableMap<String, String>
    mutablePluginsConfiguration[P::class.qualifiedName!!] = Gson().toJson(instance, T::class.java)
}

inline fun <reified P : DokkaPlugin, reified E : Any> P.query(extension: P.() -> ExtensionPoint<E>): List<E> =
    context?.let { it[extension()] } ?: throwIllegalQuery()

inline fun <reified P : DokkaPlugin, reified E : Any> P.querySingle(extension: P.() -> ExtensionPoint<E>): E =
    context?.single(extension()) ?: throwIllegalQuery()

fun throwIllegalQuery(): Nothing =
    throw IllegalStateException("Querying about plugins is only possible with dokka context initialised")

inline fun <reified T : DokkaPlugin, reified R : ConfigurableBlock> configuration(context: DokkaContext): ReadOnlyProperty<Any?, R> {
    return object : ReadOnlyProperty<Any?, R> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): R {
            return context.configuration.pluginsConfiguration.get(T::class.qualifiedName
                ?: throw AssertionError("Plugin must be named class")).let {
                    Gson().fromJson(it, R::class.java)
            }
        }
    }
}
