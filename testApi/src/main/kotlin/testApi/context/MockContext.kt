package org.jetbrains.dokka.testApi.context

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST") // It is only usable from tests so we do not care about safety
class MockContext(
    vararg extensions: Pair<ExtensionPoint<*>, Any>,
    private val testConfiguration: DokkaConfiguration? = null,
    private val testPlatforms: Map<PlatformData, EnvironmentAndFacade>? = null
) : DokkaContext {
    private val extensionMap: Map<ExtensionPoint<*>, List<Any>> =
        extensions.groupBy(Pair<ExtensionPoint<*>, Any>::first, Pair<ExtensionPoint<*>, Any>::second)

    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T? = null.also {
        logger.warn("Cannot access plugins from mock context")
    }

    override fun <T : Any, E : ExtensionPoint<T>> get(point: E): List<T> = extensionMap[point] as List<T>

    override fun <T : Any, E : ExtensionPoint<T>> single(point: E): T  = get(point).single()

    override val logger = DokkaConsoleLogger

    override val configuration: DokkaConfiguration
        get() = testConfiguration ?: throw IllegalStateException("This mock context doesn't provide configuration")

    override val platforms: Map<PlatformData, EnvironmentAndFacade>
        get() = testPlatforms ?: throw IllegalStateException("This mock context doesn't provide platforms data")
}