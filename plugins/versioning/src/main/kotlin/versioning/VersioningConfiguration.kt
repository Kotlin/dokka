package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

data class VersioningConfiguration(
    var olderVersions: List<File>? = defaultOlderVersions,
    var versionsOrdering: List<String>? = defaultVersionsOrdering,
    var version: String? = defaultVersion,
) : ConfigurableBlock {
    internal fun versionFromConfigurationOrModule(dokkaContext: DokkaContext): String =
        version ?: dokkaContext.configuration.moduleVersion ?: "1.0"

    fun olderVersionDir(dir: File) {
        olderVersions = dir.listFiles()?.toList() ?: error("Path $dir is not a directory")
    }

    companion object {
        val defaultOlderVersions: List<File>? = null
        val defaultVersionsOrdering: List<String>? = null
        val defaultVersion = null
    }
}