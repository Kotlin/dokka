package org.jetbrains.dokka.versioning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin
import java.io.File

interface VersioningHandler : () -> Unit {
    fun getVersions(): Map<VersionId, File>
    fun currentVersion(): File?
}

typealias VersionId = String

class DefaultVersioningHandler(val context: DokkaContext) : VersioningHandler {

    private val mapper = ObjectMapper()

    private lateinit var versions: Map<VersionId, File>

    private val processingStrategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private val configuration = configuration<VersioningPlugin, VersioningConfiguration>(context)

    override fun getVersions() = versions

    override fun currentVersion() = configuration?.let { versionsConfiguration ->
        versions[versionsConfiguration.versionFromConfigurationOrModule(context)]
    }

    override fun invoke() {
        configuration?.let { versionsConfiguration ->
            versions =
                mapOf(versionsConfiguration.versionFromConfigurationOrModule(context) to context.configuration.outputDir)
            handlePreviousVersions(versionsConfiguration.allOlderVersions(), context.configuration.outputDir)
            mapper.writeValue(
                context.configuration.outputDir.resolve(VERSIONS_FILE),
                Version(versionsConfiguration.versionFromConfigurationOrModule(context))
            )
        }
    }

    private fun handlePreviousVersions(olderVersions: List<File>, output: File): Map<String, File> {
        return versionsFrom(olderVersions)
            .also { fetched ->
                versions = versions + fetched.map { (key, _) ->
                    key to output.resolve(OLDER_VERSIONS_DIR).resolve(key)
                }.toMap()
            }
            .onEach { (version, path) -> copyVersion(version, path, output) }.toMap()
    }

    private fun versionsFrom(olderVersions: List<File>) =
        olderVersions.mapNotNull { versionDir ->
            versionDir.listFiles { _, name -> name == VERSIONS_FILE }?.firstOrNull()?.let { file ->
                val versionsContent = mapper.readValue<Version>(file)
                Pair(versionsContent.version, versionDir)
            }.also {
                if (it == null) context.logger.warn("Failed to find versions file named $VERSIONS_FILE in $versionDir")
            }
        }

    private fun copyVersion(version: VersionId, versionRoot: File, output: File) {
        val targetParent = output.resolve(OLDER_VERSIONS_DIR).resolve(version).apply { mkdirs() }
        val olderDirs = versionRoot.resolve(OLDER_VERSIONS_DIR)
        runBlocking(Dispatchers.Default) {
            coroutineScope {
                versionRoot.listFiles().orEmpty()
                    .filter { it.absolutePath != olderDirs.absolutePath }
                    .forEach { versionRootContent ->
                        launch {
                            if (versionRootContent.isDirectory) versionRootContent.copyRecursively(
                                targetParent.resolve(versionRootContent.name),
                                overwrite = true
                            )
                            else processingStrategies.first {
                                it.process(versionRootContent, targetParent.resolve(versionRootContent.name), null)
                            }
                        }
                    }
            }
        }
    }

    private data class Version(
        @JsonProperty("version") val version: String,
    )

    companion object {
        private const val OLDER_VERSIONS_DIR = "older"
        private const val VERSIONS_FILE = "version.json"
    }
}
