package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class BaseJsonNavigationTemplateProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    abstract val navigationFileNameWithoutExtension: String
    abstract val path: String

    private val fragments = ConcurrentHashMap<String, List<SearchRecord>>()

    open fun canProcess(file: File): Boolean =
        file.extension == "json" && file.nameWithoutExtension == navigationFileNameWithoutExtension

    override fun process(input: File, output: File, moduleContext: DokkaModuleDescription?): Boolean {
        val canProcess = canProcess(input)
        if (canProcess) {
            runCatching { parseJson<AddToSearch>(input.readText()) }.getOrNull()?.let { command ->
                moduleContext?.relativePathToOutputDirectory
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
            val content = toJsonString(fragments.entries.flatMap { (moduleName, navigation) ->
                navigation.map { it.withResolvedLocation(moduleName) }
            })
            output.resolve(path).mkdirs()
            output.resolve("$path/$navigationFileNameWithoutExtension.json").writeText(content)
        }
    }

    private fun fallbackToCopy(input: File, output: File) {
        context.logger.warn("Falling back to just copying ${input.name} file even though it should have been processed")
        input.copyTo(output)
    }

    private fun SearchRecord.withResolvedLocation(moduleName: String): SearchRecord =
        copy(location = "$moduleName/$location")

}

class NavigationSearchTemplateStrategy(val dokkaContext: DokkaContext) :
    BaseJsonNavigationTemplateProcessingStrategy(dokkaContext) {
    override val navigationFileNameWithoutExtension: String = "navigation-pane"
    override val path: String = "scripts"
}

class PagesSearchTemplateStrategy(val dokkaContext: DokkaContext) :
    BaseJsonNavigationTemplateProcessingStrategy(dokkaContext) {
    override val navigationFileNameWithoutExtension: String = "pages"
    override val path: String = "scripts"
}
