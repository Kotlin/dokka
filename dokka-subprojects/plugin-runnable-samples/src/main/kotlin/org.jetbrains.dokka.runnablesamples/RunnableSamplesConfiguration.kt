package org.jetbrains.dokka.runnablesamples

import org.jetbrains.dokka.plugability.ConfigurableBlock

public data class RunnableSamplesConfiguration(
    var kotlinPlaygroundScript: String = defaultKotlinPlaygroundScript,
    var playgroundServer: String? = null
) : ConfigurableBlock {
    public companion object {
        public const val defaultKotlinPlaygroundScript: String =
            "https://unpkg.com/kotlin-playground@1/dist/playground.min.js"
    }
}
