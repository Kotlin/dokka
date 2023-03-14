package org.jetbrains.dokka

import kotlin.jvm.Throws

interface DokkaBootstrap {
    @Throws(Throwable::class)
    fun configure(serializedConfigurationJSON: String, logger: (String, String) -> Unit)

    @Throws(Throwable::class)
    fun generate()
}
