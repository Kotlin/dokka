package org.jetbrains.dokka.testApi.context

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST") // It is only usable from tests so we do not care about safety
class MockContext(
    vararg extensions: Pair<ExtensionPoint<*>, (DokkaContext) -> Any>,
    private val testConfiguration: DokkaConfiguration? = null,
    private val testPlatforms: Map<PlatformData, EnvironmentAndFacade>? = null
) : DokkaContext {
    private val extensionMap by lazy {
        extensions.groupBy(Pair<ExtensionPoint<*>, (DokkaContext) -> Any>::first) {
            it.second(this)
        }
    }

    private val plugins = mutableMapOf<KClass<out DokkaPlugin>, DokkaPlugin>()

    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T? = plugins.getOrPut(kclass) {
        kclass.constructors.single { it.parameters.isEmpty() }.call().also { it.injectContext(this) }
    } as T

    override fun <T : Any, E : ExtensionPoint<T>> get(point: E): List<T> = extensionMap[point] as List<T>

    override fun <T : Any, E : ExtensionPoint<T>> single(point: E): T = get(point).single()

    override val logger = DokkaConsoleLogger

    override val configuration: DokkaConfiguration
        get() = testConfiguration ?: throw IllegalStateException("This mock context doesn't provide configuration")

    override val platforms: Map<PlatformData, EnvironmentAndFacade>
        get() = testPlatforms ?: throw IllegalStateException("This mock context doesn't provide platforms data")
}

private fun DokkaPlugin.injectContext(context: DokkaContext) {
    (DokkaPlugin::class.memberProperties.single { it.name == "context" } as KMutableProperty<*>)
        .setter.call(this, context)
}