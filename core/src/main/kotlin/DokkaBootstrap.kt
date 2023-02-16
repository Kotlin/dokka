package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaLogger

interface DokkaBootstrap {
    @Throws(Throwable::class)
    fun configure(serializedConfigurationJSON: String, logger: DokkaLogger)

    @Throws(Throwable::class)
    fun generate()
}
