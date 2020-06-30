package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


interface DokkaContext {
    fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T?

    operator fun <T, E> get(point: E): List<T>
            where T : Any, E : ExtensionPoint<T>

    fun <T, E> single(point: E): T where T : Any, E : ExtensionPoint<T>

    val logger: DokkaLogger
    val configuration: DokkaConfiguration
    val unusedPoints: Collection<ExtensionPoint<*>>


    companion object {
        fun create(
            configuration: DokkaConfiguration,
            logger: DokkaLogger,
            pluginOverrides: List<DokkaPlugin>
        ): DokkaContext =
            DokkaContextConfigurationImpl(logger, configuration).apply {
                // File(it.path) is a workaround for an incorrect filesystem in a File instance returned by Gradle.
                configuration.pluginsClasspath.map { File(it.path).toURI().toURL() }
                    .toTypedArray()
                    .let { URLClassLoader(it, this.javaClass.classLoader) }
                    .also { checkClasspath(it) }
                    .let { ServiceLoader.load(DokkaPlugin::class.java, it) }
                    .let { it + pluginOverrides }
                    .forEach { install(it) }
                applyExtensions()
            }.also { it.logInitialisationInfo() }
    }
}

inline fun <reified T : DokkaPlugin> DokkaContext.plugin(): T = plugin(T::class)
    ?: throw java.lang.IllegalStateException("Plugin ${T::class.qualifiedName} is not present in context.")

interface DokkaContextConfiguration {
    fun addExtensionDependencies(extension: Extension<*, *, *>)
}

private class DokkaContextConfigurationImpl(
    override val logger: DokkaLogger,
    override val configuration: DokkaConfiguration
) : DokkaContext, DokkaContextConfiguration {
    private val plugins = mutableMapOf<KClass<*>, DokkaPlugin>()
    private val pluginStubs = mutableMapOf<KClass<*>, DokkaPlugin>()
    val extensions = mutableMapOf<ExtensionPoint<*>, MutableList<Extension<*, *, *>>>()
    val pointsUsed: MutableSet<ExtensionPoint<*>> = mutableSetOf()
    val pointsPopulated: MutableSet<ExtensionPoint<*>> = mutableSetOf()
    override val unusedPoints: Set<ExtensionPoint<*>>
        get() = pointsPopulated - pointsUsed

    private enum class State {
        UNVISITED,
        VISITING,
        VISITED;
    }

    val verticesWithState = mutableMapOf<Extension<*, *, *>, State>()
    val adjacencyList: MutableMap<Extension<*, *, *>, MutableList<Extension<*, *, *>>> = mutableMapOf()

    private fun topologicalSort() {

        val result: MutableList<Extension<*, *, *>> = mutableListOf()

        fun visit(n: Extension<*, *, *>) {
            val state = verticesWithState[n]
            if (state == State.VISITED)
                return
            if (state == State.VISITING)
                throw Error("Detected cycle in plugins graph")
            verticesWithState[n] = State.VISITING
            adjacencyList[n]?.forEach { visit(it) }
            verticesWithState[n] = State.VISITED
            result += n
        }

        for ((vertex, state) in verticesWithState) {
            if (state == State.UNVISITED)
                visit(vertex)
        }
        result.asReversed().forEach {
            pointsPopulated += it.extensionPoint
            extensions.getOrPut(it.extensionPoint, ::mutableListOf) += it
        }
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T, E> get(point: E) where T : Any, E : ExtensionPoint<T> =
        actions(point).also { pointsUsed += point }.orEmpty() as List<T>

    @Suppress("UNCHECKED_CAST")
    override fun <T, E> single(point: E): T where T : Any, E : ExtensionPoint<T> {
        fun throwBadArity(substitution: String): Nothing = throw IllegalStateException(
            "$point was expected to have exactly one extension registered, but $substitution found."
        )
        pointsUsed += point

        val extensions = extensions[point].orEmpty() as List<Extension<T, *, *>>
        return when (extensions.size) {
            0 -> throwBadArity("none was")
            1 -> extensions.single().action.get(this)
            else -> throwBadArity("many were")
        }
    }

    private fun <E : ExtensionPoint<*>> actions(point: E) = extensions[point]?.map { it.action.get(this) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>) = (plugins[kclass] ?: pluginStubFor(kclass)) as T

    private fun <T : DokkaPlugin> pluginStubFor(kclass: KClass<T>): DokkaPlugin =
        pluginStubs.getOrPut(kclass) { kclass.createInstance().also { it.context = this } }

    fun install(plugin: DokkaPlugin) {
        plugins[plugin::class] = plugin
        plugin.context = this
        plugin.internalInstall(this, this.configuration)
    }

    override fun addExtensionDependencies(extension: Extension<*, *, *>) {
        if (extension.ordering is OrderingKind.ByDsl) {
            val orderDsl = OrderDsl()
            extension.ordering.block.invoke(orderDsl)

            verticesWithState += extension to State.UNVISITED
            adjacencyList.getOrPut(extension, ::mutableListOf) += orderDsl.following.toList()
            orderDsl.previous.forEach { adjacencyList.getOrPut(it, ::mutableListOf) += extension }
        }
    }

    fun logInitialisationInfo() {
        val pluginNames = plugins.values.map { it::class.qualifiedName.toString() }

        val loadedListForDebug = extensions.run { keys + values.flatten() }.toList()
            .joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") { "\t$it" }

        logger.progress("Loaded plugins: $pluginNames")
        logger.progress("Loaded: $loadedListForDebug")

    }

    fun applyExtensions() {
        topologicalSort()
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
