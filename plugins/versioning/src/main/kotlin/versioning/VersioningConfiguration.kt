package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

data class VersioningConfiguration(
    var olderVersionsDir: File? = defaultOlderVersionsDir,
    var olderVersions: List<File>? = defaultOlderVersions,
    var versionsOrdering: List<String>? = defaultVersionsOrdering,
    var version: String? = defaultVersion,
) : ConfigurableBlock {
    internal fun versionFromConfigurationOrModule(dokkaContext: DokkaContext): String =
        version ?: dokkaContext.configuration.moduleVersion ?: "1.0"

    internal fun allOlderVersions(): List<File> {
        if (olderVersionsDir != null)
            assert(olderVersionsDir!!.isDirectory) { "Supplied previous version $olderVersionsDir is not a directory!" }

        return olderVersionsDir?.listFiles()?.toList().orEmpty() + olderVersions.orEmpty()
    }

    companion object {
        val defaultOlderVersionsDir: File? = null
        val defaultOlderVersions: List<File>? = null
        val defaultVersionsOrdering: List<String>? = null
        val defaultVersion = null
    }
}