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
                .let { ServiceLoader.load(DokkaPlugin::class.java, URLClassLoader(it, this.javaClass.classLoader)) }
                .forEach { install(it) }
        }
    }
}