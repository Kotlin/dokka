package templates

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private typealias Entry = Map<String, List<String>>

class SourcesetDependencyProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    private val fileName = "sourceset_dependencies.js"
    private val fragments = ConcurrentHashMap<String, Entry>()

    override fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val content = fragments.values.fold(emptyMap<String, List<String>>()) { acc, e -> acc + e }
                .let { "sourceset_dependencies = '${toJsonString(it)}'" }
            output.resolve("scripts").mkdirs()
            output.resolve("scripts/$fileName").writeText(content)
        }
    }

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean =
        input.takeIf { it.name == fileName }
            ?.runCatching { parseJson<AddToSourcesetDependencies>(input.readText()) }
            ?.getOrNull()
            ?.also { (moduleName, content) ->
                fragments += (moduleName to content)
            } != null
}
