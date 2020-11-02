package org.jetbrains.dokka.allModulesPage.templates

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.templating.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jsoup.Jsoup
import org.jsoup.nodes.*
import org.jsoup.parser.Tag
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class DirectiveBasedTemplateProcessingStrategy(private val context: DokkaContext) : TemplateProcessingStrategy {
    private val navigationFragments = ConcurrentHashMap<String, Element>()

    private val substitutors = context.plugin<AllModulesPagePlugin>().query { substitutor }

    override suspend fun process(input: File, output: File): Unit = coroutineScope {
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
        } else {
            launch(IO) {
                Files.copy(input.toPath(), output.toPath())
            }
        }
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
        val elpFactory = context.plugin<DokkaBase>().query { externalLocationProviderFactory }

        val packageLists =
            context.configuration.modules.map { it.sourceOutputDirectory.resolve(it.relativePathToOutputDirectory) }
                .map { module ->
                    module to PackageList.load(
                        URL("file:" + module.resolve("package-list").path),
                        8,
                        true
                    )
                }.toMap()

        val externalDocumentations =
            packageLists.map { (module, pckgList) ->
                ExternalDocumentation(
                    URL("file:/${module.name}/${module.name}"),
                    pckgList!!
                )
            }

        val elps = elpFactory
            .flatMap { externalDocumentations.map { ed -> it.getExternalLocationProvider(ed) } }
            .filterNotNull()

        val absoluteLink = elps.mapNotNull { it.resolve(command.dri) }.firstOrNull()
        if (absoluteLink == null) {
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

        val modulePath = context.configuration.outputDir.absolutePath.split(File.separator)
        val contextPath = fileContext.absolutePath.split(File.separator)
        val commonPathElements = modulePath.zip(contextPath)
            .takeWhile { (a, b) -> a == b }.count()

        // -1 here to not drop the last directory
        val link =
            (List(contextPath.size - commonPathElements - 1) { ".." } + modulePath.drop(commonPathElements)).joinToString(
                "/"
            ) + absoluteLink.removePrefix("file:")

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
