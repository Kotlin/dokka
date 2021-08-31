package org.jetbrains.dokka.versioning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin
import java.io.File

class DefaultPreviousDocumentationCopyPostAction(private val context: DokkaContext) : PostAction {
    private val versioningStorage by lazy { context.plugin<VersioningPlugin>().querySingle { versioningStorage } }
    private val processingStrategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    override fun invoke() {
        versioningStorage.createVersionFile()
        versioningStorage.previousVersions.forEach { (_, dirs) -> copyVersion(dirs.src, dirs.dst) }
    }

    private fun copyVersion(versionRoot: File, targetParent: File) {
        targetParent.apply { mkdirs() }
        val ignoreDir = versionRoot.resolve(VersioningConfiguration.OLDER_VERSIONS_DIR)
        runBlocking(Dispatchers.Default) {
            coroutineScope {
                versionRoot.listFiles().orEmpty()
                    .filter { it.absolutePath != ignoreDir.absolutePath }
                    .forEach { versionRootContent ->
                        launch {
                            processRecursively(versionRootContent, targetParent)
                        }
                    }
            }
        }
    }

    private fun processRecursively(versionRootContent: File, targetParent: File) {
        if (versionRootContent.isDirectory) {
            val target = targetParent.resolve(versionRootContent.name).also { it.mkdir() }
            versionRootContent.listFiles()?.forEach {
                processRecursively(it, target)
            }
        } else if (versionRootContent.extension == "html") processingStrategies.first {
            it.process(versionRootContent, targetParent.resolve(versionRootContent.name), null)
        } else {
            versionRootContent.copyTo(targetParent.resolve(versionRootContent.name), overwrite = true)
        }
    }
}