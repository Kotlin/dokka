package org.jetbrains.dokka.templates

import kotlinx.coroutines.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jsoup.nodes.Element
import java.io.File

interface TemplateProcessor

interface SubmoduleTemplateProcessor : TemplateProcessor {
    fun process(modules: List<DokkaConfiguration.DokkaModuleDescription>): TemplatingResult
}

interface MultiModuleTemplateProcessor : TemplateProcessor {
    fun process(generatedPagesTree: RootPageNode)
}

interface TemplateProcessingStrategy {
    fun process(input: File, output: File): Boolean
    fun finish(output: File) {}
}

class DefaultSubmoduleTemplateProcessor(
    private val context: DokkaContext,
) : SubmoduleTemplateProcessor {

    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    override fun process(modules: List<DokkaConfiguration.DokkaModuleDescription>) =
        runBlocking(Dispatchers.Default) {
            coroutineScope {
                context.configuration.modules.fold(TemplatingResult()) { acc, module ->
                    acc + module.sourceOutputDirectory.visit(context.configuration.outputDir.resolve(module.relativePathToOutputDirectory))
                }
            }
        }

    private suspend fun File.visit(target: File, acc: TemplatingResult = TemplatingResult()): TemplatingResult =
        coroutineScope {
            val source = this@visit
            if (source.isDirectory) {
                target.mkdir()
                val files = source.list().orEmpty()
                val accWithSelf =
                    if (files.contains("package-list") && files.size != 1) acc.copy(modules = acc.modules + source.name)
                    else acc

                files.fold(accWithSelf) { acc, path ->
                    source.resolve(path).visit(target.resolve(path), acc)
                }
            } else {
                strategies.first { it.process(source, target) }
                acc
            }
        }
}

class DefaultMultiModuleTemplateProcessor(
    context: DokkaContext,
) : MultiModuleTemplateProcessor {
    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private val locationProviderFactory = context.plugin<DokkaBase>().querySingle { locationProviderFactory }

    override fun process(generatedPagesTree: RootPageNode) {
        val locationProvider = locationProviderFactory.getLocationProvider(generatedPagesTree)
        generatedPagesTree.withDescendants().mapNotNull { locationProvider.resolve(it)?.let { File(it) } }
            .forEach { location -> strategies.first { it.process(location, location) } }
    }

}

data class TemplatingContext<out T : Command>(
    val input: File,
    val output: File,
    val element: Element,
    val command: T,
)

data class TemplatingResult(val modules: List<String> = emptyList()) {
    operator fun plus(lhs: TemplatingResult): TemplatingResult = TemplatingResult((modules + lhs.modules).distinct())
}