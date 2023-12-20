/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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

public interface TemplateProcessor

public interface SubmoduleTemplateProcessor : TemplateProcessor {
    public fun process(modules: List<DokkaModuleDescription>): TemplatingResult
}

public interface MultiModuleTemplateProcessor : TemplateProcessor {
    public fun process(generatedPagesTree: RootPageNode)
}

public interface TemplateProcessingStrategy {
    public fun process(input: File, output: File, moduleContext: DokkaModuleDescription?): Boolean
    public fun finish(output: File) {}
}

public class DefaultSubmoduleTemplateProcessor(
    private val context: DokkaContext,
) : SubmoduleTemplateProcessor {

    private val strategies: List<TemplateProcessingStrategy> =
        context.plugin<TemplatingPlugin>().query { templateProcessingStrategy }

    private val configuredModulesPaths =
            context.configuration.modules.associate { it.sourceOutputDirectory.absolutePath to it.name }

    override fun process(modules: List<DokkaModuleDescription>): TemplatingResult {
        return runBlocking(Dispatchers.Default) {
            coroutineScope {
                modules.fold(TemplatingResult()) { acc, module ->
                    acc + module.sourceOutputDirectory.visit(context.configuration.outputDir.resolve(module.relativePathToOutputDirectory), module)
                }
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

public class DefaultMultiModuleTemplateProcessor(
    public val context: DokkaContext,
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

public data class TemplatingContext<out T : Command>(
    val input: File,
    val output: File,
    val body: List<Node>,
    val command: T,
)

public data class TemplatingResult(val modules: List<String> = emptyList()) {
    public operator fun plus(rhs: TemplatingResult): TemplatingResult {
        return TemplatingResult((modules + rhs.modules).distinct())
    }
}
