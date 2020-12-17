package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.jetbrains.dokka.base.templating.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.concurrent.ConcurrentHashMap

abstract class BaseJsonNavigationTemplateProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    abstract val navigationFileNameWithoutExtension: String
    abstract val path: String

    private val fragments = ConcurrentHashMap<String, List<SearchRecord>>()

    open fun canProcess(file: File): Boolean =
        file.extension == "json" && file.nameWithoutExtension == navigationFileNameWithoutExtension

    override suspend fun process(input: File, output: File): Boolean = coroutineScope {
        val canProcess = canProcess(input)
        if (canProcess) {
            launch {
                withContext(Dispatchers.IO) {
                    runCatching { parseJson<AddToSearch>(input.readText()) }.getOrNull()
                }?.let { command ->
                    fragments[command.moduleName] = command.elements
                } ?: fallbackToCopy(input, output)
            }
        }
        canProcess
    }

    override suspend fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val content = toJsonString(fragments.entries.flatMap { (moduleName, navigation) ->
                navigation.map { it.withResolvedLocation(moduleName) }
            })
            withContext(Dispatchers.IO) {
                output.resolve("$path/$navigationFileNameWithoutExtension.json").writeText(content)

                fragments.keys.forEach {
                    output.resolve(it).resolve("$path/$navigationFileNameWithoutExtension.json").writeText(content)
                }
            }
        }
    }

    private suspend fun fallbackToCopy(input: File, output: File) {
        context.logger.warn("Falling back to just copying file for ${input.name} even thought it should process it")
        withContext(Dispatchers.IO) { input.copyTo(output) }
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