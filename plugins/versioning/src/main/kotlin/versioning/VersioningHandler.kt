package org.jetbrains.dokka.versioning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import java.io.File

interface VersioningHandler {
    val versions: Map<VersionId, File>
    val previousVersions: Map<VersionId, VersionDirs>
    fun currentVersion(): File?
}

data class VersionDirs(val src: File, val dst: File)
typealias VersionId = String

class DefaultVersioningHandler(val context: DokkaContext) : VersioningHandler {

    private val mapper = ObjectMapper()
    private val configuration = configuration<VersioningPlugin, VersioningConfiguration>(context)

    override val previousVersions: Map<VersionId, VersionDirs> by lazy {
        configuration?.let { versionsConfiguration ->
            handlePreviousVersions(versionsConfiguration.allOlderVersions(), context.configuration.outputDir)
        } ?: emptyMap()
    }

    private val currentVersionId: Pair<VersionId, File> by lazy {
        configuration?.let { versionsConfiguration ->
            versionsConfiguration.versionFromConfigurationOrModule(context) to context.configuration.outputDir
        }?.also {
            mapper.writeValue(
                it.second.resolve(VersioningConfiguration.VERSIONS_FILE),
                Version(it.first)
            )
        } ?: (context.configuration.moduleVersion.orEmpty() to context.configuration.outputDir)
    }
    override val versions: Map<VersionId, File> by lazy {
        previousVersions.map { (k, v) -> k to v.dst }.toMap() + currentVersionId
    }


    override fun currentVersion() = currentVersionId.second

    private fun handlePreviousVersions(olderVersions: List<File>, output: File): Map<String, VersionDirs> =
        versionsFrom(olderVersions).associate { (key, srcDir) ->
            key to VersionDirs(srcDir, output.resolve(VersioningConfiguration.OLDER_VERSIONS_DIR).resolve(key))
        }

    private fun versionsFrom(olderVersions: List<File>) =
        olderVersions.mapNotNull { versionDir ->
            versionDir.listFiles { _, name -> name == VersioningConfiguration.VERSIONS_FILE }?.firstOrNull()
                ?.let { file ->
                    val versionsContent = mapper.readValue<Version>(file)
                    Pair(versionsContent.version, versionDir)
                }.also {
                    if (it == null) context.logger.warn("Failed to find versions file named ${VersioningConfiguration.VERSIONS_FILE} in $versionDir")
                }
        }

    private data class Version(
        @JsonProperty("version") val version: String,
    )
}
