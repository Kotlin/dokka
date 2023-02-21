package org.jetbrains.dokka.templates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jsoup.nodes.Node
import java.io.File

interface TemplateProcessor

interface SubmoduleTemplateProcessor : TemplateProcessor {
    fun process(modules: List<DokkaModuleDescription>): TemplatingResult
}

interface MultiModuleTemplateProcessor : TemplateProcessor {
    fun process(generatedPagesTree: RootPageNode)
}

interface TemplateProcessingStrategy {
    fun process(input: File, output: File, moduleContext: DokkaModuleDescription?): Boolean
    fun finish(output: File) {}
}

class DefaultSubmoduleTemplateProcessor(
    private val context: DokkaContext,
) : SubmoduleTemplateProcessor {

    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private val configuredModulesPaths =
            context.configuration.modules.associate { it.sourceOutputDirectory.absolutePath to it.name }

    override fun process(modules: List<DokkaModuleDescription>) =
        runBlocking(Dispatchers.Default) {
            coroutineScope {
                modules.fold(TemplatingResult()) { acc, module ->
                    acc + module.sourceOutputDirectory.visit(context.configuration.outputDir.resolve(module.relativePathToOutputDirectory), module)
                }
            }
        }

    private suspend fun File.visit(target: File, module: DokkaModuleDescription, acc: TemplatingResult = TemplatingResult()): TemplatingResult =
        coroutineScope {
            val source = this@visit
            if (source.isDirectory) {
                target.mkdirs()
                val files = source.list().orEmpty()
                val accWithSelf = configuredModulesPaths[source.absolutePath]
                    ?.takeIf { files.firstOrNull { !it.startsWith(".") } != null }
                    ?.let { acc.copy(modules = acc.modules + it) }
                    ?: acc

                files.fold(accWithSelf) { acc, path ->
                    source.resolve(path).visit(target.resolve(path), module, acc)
                }
            } else {
                strategies.first { it.process(source, target, module) }
                acc
            }
        }
}

class DefaultMultiModuleTemplateProcessor(
    val context: DokkaContext,
) : MultiModuleTemplateProcessor {
    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private val locationProviderFactory = context.plugin<DokkaBase>().querySingle { locationProviderFactory }

    override fun process(generatedPagesTree: RootPageNode) {
        val locationProvider = locationProviderFactory.getLocationProvider(generatedPagesTree)
        generatedPagesTree.withDescendants().mapNotNull { pageNode -> locationProvider.resolve(pageNode)?.let { File(it) } }
            .forEach { location -> strategies.first { it.process(location, location, null) } }
    }
}

data class TemplatingContext<out T : Command>(
    val input: File,
    val output: File,
    val body: List<Node>,
    val command: T,
)

data class TemplatingResult(val modules: List<String> = emptyList()) {
    operator fun plus(rhs: TemplatingResult): TemplatingResult = TemplatingResult((modules + rhs.modules).distinct())
}
