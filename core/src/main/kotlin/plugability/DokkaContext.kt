package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaLogger
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

interface DokkaContext {
    operator fun <T, E> get(point: E, askDefault: AskDefault = AskDefault.WhenEmpty): List<Extension<T>>
            where T : Any, E : ExtensionPoint<T>

    fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T?

    val logger: DokkaLogger

    companion object {
        fun create(pluginsClasspath: Iterable<File>, logger: DokkaLogger): DokkaContext =
            DokkaContextConfigurationImpl(logger, DefaultContext(logger)).apply {
                pluginsClasspath.map { it.relativeTo(File(".").absoluteFile).toURI().toURL() }
                    .toTypedArray()
                    .let { URLClassLoader(it, this.javaClass.classLoader) }
                    .also { checkClasspath(it) }
                    .let { ServiceLoader.load(DokkaPlugin::class.java, it) }
                    .forEach { install(it) }
            }.also { it.logInitialisationInfo() }
    }
}

interface DokkaContextConfiguration {
    fun addExtension(extension: Extension<*>)
}

private class DokkaContextConfigurationImpl(
    override val logger: DokkaLogger,
    private val defaultContext: DokkaContext?
) : DokkaContext, DokkaContextConfiguration {
    private val plugins = mutableMapOf<KClass<*>, DokkaPlugin>()

    private val pluginStubs = mutableMapOf<KClass<*>, DokkaPlugin>()

    internal val extensions = mutableMapOf<ExtensionPoint<*>, MutableList<Extension<*>>>()

    @Suppress("UNCHECKED_CAST")
    override operator fun <T, E> get(point: E, askDefault: AskDefault) where T : Any, E : ExtensionPoint<T> =
        when (askDefault) {
            AskDefault.Never -> extensions[point].orEmpty()
            AskDefault.Always -> extensions[point].orEmpty() + defaultContext?.get(point, askDefault).orEmpty()
            AskDefault.WhenEmpty ->
                extensions[point]?.takeIf { it.isNotEmpty() } ?: defaultContext?.get(point, askDefault).orEmpty()
        } as List<Extension<T>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>) =
        (plugins[kclass] ?: defaultContext?.plugin(kclass) ?: pluginStubFor(kclass)) as T

    private fun <T : DokkaPlugin> pluginStubFor(kclass: KClass<T>): DokkaPlugin =
        pluginStubs.getOrPut(kclass) { kclass.createInstance().also { it.context = this } }

    fun install(plugin: DokkaPlugin) {
        plugins[plugin::class] = plugin
        plugin.context = this
        plugin.internalInstall(this)
    }

    override fun addExtension(extension: Extension<*>) {
        extensions.getOrPut(extension.extensionPoint, ::mutableListOf) += extension
    }

    fun logInitialisationInfo() {
        val pluginNames: List<String> = plugins.values.map { it::class.qualifiedName.toString() }

        val loadedListForDebug = extensions.run { keys + values.flatten() }.toList()
            .joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") { "\t$it" }

        logger.progress("Loaded plugins: $pluginNames")
        logger.progress("Loaded: $loadedListForDebug")

    }
}

private fun checkClasspath(classLoader: URLClassLoader) {
    classLoader.findResource(DokkaContext::class.java.name.replace('.', '/') + ".class")?.also {
        throw AssertionError(
            "Dokka API found on plugins classpath. This will lead to subtle bugs. " +
                    "Please fix your plugins dependencies or exclude dokka api artifact from plugin classpath"
        )
    }
}

class DefaultContext(override val logger: DokkaLogger) : DokkaContext {
    override fun <T : Any, E : ExtensionPoint<T>> get(point: E, askDefault: AskDefault): List<Extension<T>> =
        when (point) {

            else -> emptyList()
        }

    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>): Nothing? = null
}

enum class AskDefault {
    Always, Never, WhenEmpty
}