package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.base.renderers.html.JsSerializer
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class BaseJsNavigationTemplateProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    abstract val navigationFileNameWithoutExtension: String
    abstract val path: String

    private val fragments = ConcurrentHashMap<String, List<SearchRecord>>()
    private val extension = "js"

    open fun canProcess(file: File): Boolean =
        file.extension == extension && file.nameWithoutExtension == navigationFileNameWithoutExtension

    /**
     * Entry point for processing data
     *
     * For sake of convenience data is formatted as json and then read in type-safe way.
     * After that it is saved to desired format, in this case js files, using [finish] method
     */
    override fun process(input: File, output: File): Boolean {
        val canProcess = canProcess(input)
        if (canProcess) {
            runCatching { parseJson<AddToSearch>(input.readText()) }.getOrNull()?.let { command ->
                context.configuration.modules.find { it.name == command.moduleName }?.relativePathToOutputDirectory
                    ?.relativeToOrSelf(context.configuration.outputDir)
                    ?.let { key ->
                        fragments[key.toString()] = command.elements
                    }
            } ?: fallbackToCopy(input, output)
        }
        return canProcess
    }

    override fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val content = JsSerializer().serialize(fragments.entries.flatMap { (moduleName, navigation) ->
                navigation.map { it.withResolvedLocation(moduleName) }
            })
            output.resolve(path).mkdirs()
            output.resolve("$path/$navigationFileNameWithoutExtension.$extension").writeText(content)
        }
    }

    private fun fallbackToCopy(input: File, output: File) {
        context.logger.warn("Falling back to just copying file for ${input.name} even thought it should process it")
        input.copyTo(output)
    }

    private fun SearchRecord.withResolvedLocation(moduleName: String): SearchRecord =
        copy(location = "$moduleName/$location")

}

class NavigationSearchTemplateStrategy(val dokkaContext: DokkaContext) :
    BaseJsNavigationTemplateProcessingStrategy(dokkaContext) {
    override val navigationFileNameWithoutExtension: String = "navigation-pane"
    override val path: String = "scripts"
}

class PagesSearchTemplateStrategy(val dokkaContext: DokkaContext) :
    BaseJsNavigationTemplateProcessingStrategy(dokkaContext) {
    override val navigationFileNameWithoutExtension: String = "pages"
    override val path: String = "scripts"
}