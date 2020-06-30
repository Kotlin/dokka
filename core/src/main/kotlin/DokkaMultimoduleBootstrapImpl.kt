package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaBootstrapImpl.DokkaProxyLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.function.BiConsumer

class DokkaMultimoduleBootstrapImpl : DokkaBootstrap {

    private lateinit var generator: DokkaGenerator

    fun configure(logger: DokkaLogger, configuration: DokkaConfiguration) {
        generator = DokkaGenerator(configuration, logger)
    }

    override fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String) = configure(
        DokkaProxyLogger(logger),
        DokkaConfigurationImpl(serializedConfigurationJSON)
    )

    override fun generate() {
        generator.generateAllModulesPage()
    }

}
