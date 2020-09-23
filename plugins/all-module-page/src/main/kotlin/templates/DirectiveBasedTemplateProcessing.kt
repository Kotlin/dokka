package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
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
import java.util.concurrent.ConcurrentHashMap

class DirectiveBasedTemplateProcessingStrategy(private val context: DokkaContext) : TemplateProcessingStrategy {
    private val navigationFragments = ConcurrentHashMap<String, Element>()

    override suspend fun process(input: File, output: File): Unit = coroutineScope {
        if (input.extension == "html") {
            launch {
                val document = withContext(IO) { Jsoup.parse(input, "UTF-8") }
                document.outputSettings().indentAmount(0).prettyPrint(false)
                document.select("dokka-template-command").forEach {
                    val command = parseJson<Command>(it.attr("data"))
                    when (command) {
                        is ResolveLinkCommand -> resolveLink(it, command)
                        is AddToNavigationCommand -> navigationFragments[command.moduleName] = it
                        else -> context.logger.warn("Unknown templating command $command")
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

    override suspend fun finish(output: File) {
        val attributes = Attributes().apply {
            put("class", "sideMenu")
        }
        val node = Element(Tag.valueOf("div"), "", attributes)
        navigationFragments.entries.sortedBy { it.key }.forEach { (moduleName, command) ->
            command.select("a").forEach { a ->
                a.attr("href")?.also { a.attr("href", "${moduleName}/${it}") }
            }
            command.childNodes().toList().forEachIndexed { index, child ->
                if (index == 0) {
                    child.attr("id", "$moduleName-nav-submenu")
                }
                node.appendChild(child)
            }
        }

        withContext(IO) {
            Files.writeString(output.resolve("navigation.html").toPath(), node.outerHtml())
        }

        node.select("a").forEach { a ->
            a.attr("href")?.also { a.attr("href", "../${it}") }
        }
        navigationFragments.keys.forEach {
            withContext(IO) {
                Files.writeString(
                    output.resolve(it).resolve("navigation.html").toPath(),
                    node.outerHtml()
                )
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