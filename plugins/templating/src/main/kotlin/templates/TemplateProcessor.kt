package org.jetbrains.dokka.templates

import kotlinx.coroutines.*
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jsoup.nodes.Element
import java.io.File

interface TemplateProcessor {
    fun process(): TemplatingResult
}

interface TemplateProcessingStrategy {
    fun process(input: File, output: File): Boolean
    fun finish(output: File) {}
}

class DefaultTemplateProcessor(
    private val context: DokkaContext,
) : TemplateProcessor {

    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    override fun process() = runBlocking(Dispatchers.Default) {
        val templatingResult = coroutineScope {
            context.configuration.modules.foldRight(TemplatingResult()) { module, acc ->
                acc + module.sourceOutputDirectory.visit(context.configuration.outputDir.resolve(module.relativePathToOutputDirectory))
            }
        }
        strategies.map { it.finish(context.configuration.outputDir) }
        templatingResult
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

                files.foldRight(accWithSelf) { path, acc ->
                    withContext(Dispatchers.Default) {
                        source.resolve(path).visit(target.resolve(path), acc)
                    }
                }
            } else {
                strategies.first { it.process(source, target) }
                acc
            }
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