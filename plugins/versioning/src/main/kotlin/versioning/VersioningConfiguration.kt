package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

data class VersioningConfiguration(
    var olderVersionsDir: File? = defaultOlderVersionsDir,
    var versionsOrdering: List<String>? = defaultVersionsOrdering,
    var version: String? = defaultVersion,
) : ConfigurableBlock {
    fun versionFromConfigurationOrModule(dokkaContext: DokkaContext): String =
        version ?: dokkaContext.configuration.moduleVersion ?: "1.0"

    companion object {
        val defaultOlderVersionsDir: File? = null
        val defaultVersionsOrdering: List<String>? = null
        val defaultVersion = null
    }
}