package org.jetbrains.dokka.plugability

import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.KClass


class DokkaContext private constructor() {
    private val plugins = mutableMapOf<KClass<*>, DokkaPlugin>()

    internal val extensions = mutableMapOf<ExtensionPoint<*>, MutableList<Extension<*>>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T: Any, E: ExtensionPoint<T>> get(point: E) = extensions[point] as List<Extension<T>>

    @PublishedApi
    internal fun plugin(kclass: KClass<*>) = plugins[kclass]

    val pluginNames: List<String>
        get() = plugins.values.map { it::class.qualifiedName.toString() }

    val loadedListForDebug
        get() = extensions.run { keys + values.flatten() }.toList()
            .joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") { "\t$it" }

    private fun install(plugin: DokkaPlugin) {
        plugins[plugin::class] = plugin
        plugin.internalInstall(this)
    }

    companion object {
        fun from(pluginsClasspath: Iterable<File>) = DokkaContext().apply {
            pluginsClasspath.map { it.relativeTo(File(".").absoluteFile).toURI().toURL() }
                .toTypedArray()
                .let { URLClassLoader(it, this.javaClass.classLoader) }
                .also { checkClasspath(it) }
                .let { ServiceLoader.load(DokkaPlugin::class.java, it) }
                .forEach { install(it) }
        }
    }

    private fun checkClasspath(classLoader: URLClassLoader) {
        classLoader.findResource(javaClass.name.replace('.', '/') + ".class")?.also {
            throw AssertionError(
                "Dokka API found on plugins classpath. This will lead to subtle bugs. " +
                        "Please fix your plugins dependencies or exclude dokka api artifact from plugin classpath"
            )
        }
    }

    internal fun addExtension(it: Extension<*>) {
        extensions.getOrPut(it.extensionPoint, ::mutableListOf) += it
    }
}
