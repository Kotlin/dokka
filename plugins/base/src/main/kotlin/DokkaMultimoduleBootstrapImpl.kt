package org.jetbrains.dokka.base

import com.google.gson.Gson
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.function.BiConsumer

class DokkaMultimoduleBootstrapImpl : DokkaBootstrap {

    private lateinit var generator: DokkaGenerator

    fun configure(logger: DokkaLogger, configuration: DokkaConfiguration) {
        generator = DokkaGenerator(configuration, logger)
    }

    override fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String) = configure(
        DokkaBootstrapImpl.DokkaProxyLogger(logger),
        Gson().fromJson(serializedConfigurationJSON, DokkaConfigurationImpl::class.java)
    )

    override fun generate() {
        generator.generateAllModulesPage()
    }

}