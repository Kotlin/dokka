package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.base.templating.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jsoup.Jsoup
import org.jsoup.nodes.*
import org.jsoup.parser.Tag
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class DirectiveBasedHtmlTemplateProcessingStrategy(private val context: DokkaContext) : TemplateProcessingStrategy {
    private val navigationFragments = ConcurrentHashMap<String, Element>()

    private val substitutors = context.plugin<AllModulesPagePlugin>().query { substitutor }
    private val externalModuleLinkResolver = ExternalModuleLinkResolver(context)

    override suspend fun process(input: File, output: File): Boolean = coroutineScope {
        if (input.extension == "html") {
            launch {
                val document = withContext(IO) { Jsoup.parse(input, "UTF-8") }
                document.outputSettings().indentAmount(0).prettyPrint(false)
                document.select("dokka-template-command").forEach {
                    when (val command = parseJson<Command>(it.attr("data"))) {
                        is ResolveLinkCommand -> resolveLink(it, command, output)
                        is AddToNavigationCommand -> navigationFragments[command.moduleName] = it
                        is SubstitutionCommand -> substitute(it, TemplatingContext(input, output, it, command))
                        else -> context.logger.warn("Unknown templating command $command")
                    }
                }
                withContext(IO) { Files.write(output.toPath(), listOf(document.outerHtml())) }
            }
            true
        } else false
    }

    private fun substitute(element: Element, commandContext: TemplatingContext<SubstitutionCommand>) {
        val regex = commandContext.command.pattern.toRegex()
        element.children().forEach { it.traverseToSubstitute(regex, commandContext) }

        val childrenCopy = element.children().toList()
        val position = element.elementSiblingIndex()
        val parent = element.parent()
        element.remove()

        parent.insertChildren(position, childrenCopy)
    }

    private fun Node.traverseToSubstitute(regex: Regex, commandContext: TemplatingContext<SubstitutionCommand>) {
        when (this) {
            is TextNode -> replaceWith(TextNode(wholeText.substitute(regex, commandContext)))
            is DataNode -> replaceWith(DataNode(wholeData.substitute(regex, commandContext)))
            is Element -> {
                attributes().forEach { attr(it.key, it.value.substitute(regex, commandContext)) }
                childNodes().forEach { it.traverseToSubstitute(regex, commandContext) }
            }
        }
    }

    private fun String.substitute(regex: Regex, commandContext: TemplatingContext<SubstitutionCommand>) = buildString {
        var lastOffset = 0
        regex.findAll(this@substitute).forEach { match ->
            append(this@substitute, lastOffset, match.range.first)
            append(findSubstitution(commandContext, match))
            lastOffset = match.range.last + 1
        }
        append(this@substitute, lastOffset, this@substitute.length)
    }

    private fun findSubstitution(commandContext: TemplatingContext<SubstitutionCommand>, match: MatchResult): String =
        substitutors.asSequence().mapNotNull { it.trySubstitute(commandContext, match) }.firstOrNull() ?: match.value

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
            Files.write(output.resolve("navigation.html").toPath(), listOf(node.outerHtml()))
        }

        node.select("a").forEach { a ->
            a.attr("href")?.also { a.attr("href", "../${it}") }
        }
        navigationFragments.keys.forEach {
            withContext(IO) {
                Files.write(
                    output.resolve(it).resolve("navigation.html").toPath(),
                    listOf(node.outerHtml())
                )
            }
        }
    }

    private fun resolveLink(it: Element, command: ResolveLinkCommand, fileContext: File) {

        val link = externalModuleLinkResolver.resolve(command.dri, fileContext)
        if (link == null) {
            val children = it.childNodes().toList()
            val attributes = Attributes().apply {
                put("data-unresolved-link", command.dri.toString())
            }
            val element = Element(Tag.valueOf("span"), "", attributes).apply {
                children.forEach { ch -> appendChild(ch) }
            }
            it.replaceWith(element)
            return
        }

        val attributes = Attributes().apply {
            put("href", link) // TODO: resolve
        }
        val children = it.childNodes().toList()
        val element = Element(Tag.valueOf("a"), "", attributes).apply {
            children.forEach { ch -> appendChild(ch) }
        }
        it.replaceWith(element)
    }
}
