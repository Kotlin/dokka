package org.jetbrains.dokka.versioning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin
import java.io.File
import java.nio.file.Path

interface VersioningHandler : () -> Unit {
    fun getVersions(): Map<String, Path>
    fun currentVersion(): Path?
}

class DefaultVersioningHandler(val context: DokkaContext) : VersioningHandler {

    private val mapper = ObjectMapper()

    private val processingStrategies: List<TemplateProcessingStrategy> = context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private var versions = mutableMapOf<String, Path>()

    private val configuration = configuration<VersioningPlugin, VersioningConfiguration>(context)

    override fun getVersions() = versions

    override fun currentVersion() = configuration?.let { versionsConfiguration ->
        versions[versionsConfiguration.currentVersion]
    }

    override fun invoke() {
        versions = mutableMapOf()
        configuration?.let { versionsConfiguration ->
            val output = context.configuration.outputDir
            val version = versionsConfiguration.currentVersion
            addVersionDir(version, output)
            versionsConfiguration.olderVersions?.let {
                handlePreviousVersions(it, output)
            }
            mapper.writeValue(output.resolve(VERSIONS_FILE), Version(version, versions.keys - version))
        }
    }

    private fun addVersionDir(version: String, dir: File) {
        versions[version] = dir.resolve("index.html").toPath()
    }

    private fun handlePreviousVersions(olderVersionDir: File, output: File) {
        assert(olderVersionDir.isDirectory) { "Supplied previous version $olderVersionDir is not a directory!" }
        val children = olderVersionDir.listFiles().orEmpty()
        val oldVersion = children.first { it.name == VERSIONS_FILE }.let { file ->
            mapper.readValue(file, Version::class.java)
        }
        val olderVersionsDir = output.resolve(OLDER_VERSIONS_DIR).apply { mkdir() }
        val previousVersionDir = olderVersionsDir.resolve(oldVersion.current).apply { mkdir() }
        addVersionDir(oldVersion.current, previousVersionDir)
        oldVersion.previous.forEach { addVersionDir(it, olderVersionsDir.resolve(it).apply { mkdir() }) }
        runBlocking(Dispatchers.Default) {
            coroutineScope {
                suspend fun processAndCopy(file: File, targetParent: File) {
                    if (file.isDirectory) file.copyRecursively(targetParent, overwrite = true)
                    else processingStrategies.first {
                        it.process(file, targetParent)
                    }
                }
                children.forEach { file ->
                    launch {
                        when (file.name) {
                            OLDER_VERSIONS_DIR -> file.listFiles()?.forEach { historicalVersionDirName ->
                                val targetDir = olderVersionsDir.resolve(historicalVersionDirName)
                                val historicalVersionDir = file.resolve(historicalVersionDirName)
                                historicalVersionDir.listFiles()?.forEach {
                                    processAndCopy(historicalVersionDir.resolve(it), targetDir.resolve(it))
                                }
                            }
                            else -> processAndCopy(file, previousVersionDir.resolve(file.name))
                        }
                    }
                }
            }
        }
    }

    private data class Version(
        @JsonProperty("current") val current: String,
        @JsonProperty("previous") val previous: Collection<String>
    )

    companion object {
        private const val OLDER_VERSIONS_DIR = "older"
        private const val VERSIONS_FILE = "version.json"
    }
}