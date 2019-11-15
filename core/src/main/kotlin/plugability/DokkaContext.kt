package org.jetbrains.dokka.plugability

import java.io.File
import java.net.URLClassLoader
import java.util.*


class DokkaContext private constructor() {
    private val plugins = mutableListOf<DokkaPlugin>()

    val pluginNames: List<String>
        get() = plugins.map { it.name }

    private fun install(plugin: DokkaPlugin) {
        plugins += plugin
        plugin.install(this)
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
        classLoader.findResource(javaClass.name.replace('.','/') + ".class")?.also {
            throw AssertionError("Dokka API found on plugins classpath. This will lead to subtle bugs. " +
                    "Please fix your plugins dependencies or exclude dokka api artifact from plugin classpath")
        }
    }
}
