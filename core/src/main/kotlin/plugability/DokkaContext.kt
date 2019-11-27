package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


interface DokkaExtensionHandler {
    operator fun <T, E> get(point: E, askDefault: AskDefault = AskDefault.WhenEmpty): List<T>
            where T : Any, E : ExtensionPoint<T>

}

interface DokkaContext : DokkaExtensionHandler {
    fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T?

    val logger: DokkaLogger

    val platforms: Map<PlatformData, EnvironmentAndFacade>

    companion object {
        fun create(
            pluginsClasspath: Iterable<File>,
            logger: DokkaLogger,
            platforms: Map<PlatformData, EnvironmentAndFacade>
        ): DokkaContext =
            DokkaContextConfigurationImpl(logger, DefaultExtensions, platforms).apply {
                pluginsClasspath.map { it.relativeTo(File(".").absoluteFile).toURI().toURL() }
                    .toTypedArray()
                    .let { URLClassLoader(it, this.javaClass.classLoader) }
                    .also { checkClasspath(it) }
                    .let { ServiceLoader.load(DokkaPlugin::class.java, it) }
                    .forEach { install(it) }
            }.also { it.logInitialisationInfo() }
    }
}

fun <T, E> DokkaContext.single(point: E): T where T : Any, E : ExtensionPoint<T> {
    fun throwBadArity(substitution: String): Nothing = throw IllegalStateException(
        "$point was expected to have exactly one extension registered, but $substitution found."
    )

    val extensions = get(point, AskDefault.WhenEmpty)
    return when (extensions.size) {
        0 -> throwBadArity("none was")
        1 -> extensions.first()
        else -> throwBadArity("multiple were")
    }
}

interface DokkaContextConfiguration {
    fun addExtension(extension: Extension<*>)
}

private class DokkaContextConfigurationImpl(
    override val logger: DokkaLogger,
    private val defaultHandler: DokkaExtensionHandler?,
    override val platforms: Map<PlatformData, EnvironmentAndFacade>
) : DokkaContext, DokkaContextConfiguration {
    private val plugins = mutableMapOf<KClass<*>, DokkaPlugin>()

    private val pluginStubs = mutableMapOf<KClass<*>, DokkaPlugin>()

    internal val extensions = mutableMapOf<ExtensionPoint<*>, MutableList<Extension<*>>>()

    @Suppress("UNCHECKED_CAST")
    override operator fun <T, E> get(point: E, askDefault: AskDefault) where T : Any, E : ExtensionPoint<T> =
        when (askDefault) {
            AskDefault.Never -> actions(point).orEmpty()
            AskDefault.Always -> actions(point).orEmpty() + defaultHandler?.get(point, askDefault).orEmpty()
            AskDefault.WhenEmpty ->
                actions(point)?.takeIf { it.isNotEmpty() } ?: defaultHandler?.get(point, askDefault).orEmpty()
        } as List<T>

    private fun <E : ExtensionPoint<*>> actions(point: E) = extensions[point]?.map { it.action }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>) = (plugins[kclass] ?: pluginStubFor(kclass)) as T

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
        val pluginNames = plugins.values.map { it::class.qualifiedName.toString() }

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

enum class AskDefault {
    Always, Never, WhenEmpty
}