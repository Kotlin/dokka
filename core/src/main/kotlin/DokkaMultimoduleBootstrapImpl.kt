/**
 * Accessed with reflection
 */
@file:Suppress("unused")

package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaBootstrapImpl.DokkaProxyLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.function.BiConsumer

class DokkaMultimoduleBootstrapImpl : DokkaBootstrap {
    override fun generate(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) {
        val generator = DokkaGenerator(DokkaProxyLogger(logger))
        val multiModuleConfiguration = DokkaMultiModuleConfigurationImpl(serializedConfigurationJSON)
        for (moduleConfiguration in multiModuleConfiguration.modules) {
            generator.generate(moduleConfiguration)
        }
        generator.generateAllModulesPage(multiModuleConfiguration)
    }
}
