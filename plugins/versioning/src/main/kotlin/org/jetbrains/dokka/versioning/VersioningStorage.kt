package org.jetbrains.dokka.versioning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import java.io.File

data class VersionDirs(val src: File, val dst: File)
data class CurrentVersion(val name: String, val dir: File)

interface VersioningStorage {
    val previousVersions: Map<VersionId, VersionDirs>
    val currentVersion: CurrentVersion
    fun createVersionFile()
}

typealias VersionId = String

class DefaultVersioningStorage(val context: DokkaContext) : VersioningStorage {

    private val mapper = ObjectMapper()
    private val configuration = configuration<VersioningPlugin, VersioningConfiguration>(context)

    override val previousVersions: Map<VersionId, VersionDirs> by lazy {
        configuration?.let { versionsConfiguration ->
            getPreviousVersions(versionsConfiguration.allOlderVersions(), context.configuration.outputDir)
        } ?: emptyMap()
    }

    override val currentVersion: CurrentVersion by lazy {
        configuration?.let { versionsConfiguration ->
            CurrentVersion(versionsConfiguration.versionFromConfigurationOrModule(context),
                context.configuration.outputDir)
        }?: CurrentVersion(context.configuration.moduleVersion.orEmpty(), context.configuration.outputDir)
    }

    override fun createVersionFile() {
        mapper.writeValue(
            currentVersion.dir.resolve(VersioningConfiguration.VERSIONS_FILE),
            Version(currentVersion.name)
        )
    }

    private fun getPreviousVersions(olderVersions: List<File>, output: File): Map<String, VersionDirs> =
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
