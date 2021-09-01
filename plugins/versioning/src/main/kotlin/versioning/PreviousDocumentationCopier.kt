package versioning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.PostAction
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import java.io.File

class DefaultPreviousDocumentationCopier(private val context: DokkaContext) : PostAction {
    private val versioningHandler by lazy { context.plugin<VersioningPlugin>().querySingle { versioningHandler } }
    private val processingStrategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    override fun invoke() {
        versioningHandler.previousVersions.onEach { (_, dirs) -> copyVersion(dirs.src, dirs.dst) }
            .toMap()
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