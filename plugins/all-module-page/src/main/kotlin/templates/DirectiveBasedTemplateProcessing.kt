package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ResolveLinkCommand
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.plugability.DokkaContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.io.File
import java.nio.file.Files

class DirectiveBasedTemplateProcessingStrategy(context: DokkaContext) : TemplateProcessingStrategy {
    override suspend fun process(input: File, output: File): Unit = coroutineScope {
        if (input.extension == "html") {
            launch {
                val document = withContext(IO) { Jsoup.parse(input, "UTF-8") }
                document.outputSettings().indentAmount(0).prettyPrint(false)
                document.select("dokka-template-command").forEach {
                    val command = parseJson<Command>(it.attr("data"))
                    if (command is ResolveLinkCommand) {
                        resolveLink(it, command)
                    }
                }
                withContext(IO) { Files.writeString(output.toPath(), document.outerHtml()) }
            }
        } else {
            launch(IO) {
                Files.copy(input.toPath(), output.toPath())
            }
        }
    }

    private fun resolveLink(it: Element, command: ResolveLinkCommand) {
        val attributes = Attributes().apply {
            put("href", "") // TODO: resolve
        }
        val children = it.childNodes().toList()
        val element = Element(Tag.valueOf("a"), "", attributes).apply {
            children.forEach { ch -> appendChild(ch) }
        }
        it.replaceWith(element)
    }
}