package org.jetbrains.dokka

import java.util.function.BiConsumer

interface DokkaBootstrap {
    fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>)
    fun generate()
}
