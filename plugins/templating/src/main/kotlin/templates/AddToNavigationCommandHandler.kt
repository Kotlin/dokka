package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.plugability.DokkaContext
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.io.File
import java.nio.file.Files
import java.util.Collections

class AddToNavigationCommandHandler(val context: DokkaContext) : CommandHandler {
    private val navigationFragments = Collections.synchronizedList(mutableListOf<Pair<String, Element>>())

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        command as AddToNavigationCommand
        context.configuration.modules.find { it.name == command.moduleName }
            ?.relativePathToOutputDirectory
            ?.relativeToOrSelf(context.configuration.outputDir)
            ?.let { key -> navigationFragments.add(Pair(key.toString(), body)) }
    }

    override fun canHandle(command: Command) = command is AddToNavigationCommand

    override fun finish(output: File) {
        if (navigationFragments.isNotEmpty()) {
            val attributes = Attributes().apply {
                put("class", "sideMenu")
            }
            val node = Element(Tag.valueOf("div"), "", attributes)
            navigationFragments.forEach { (moduleName, command) ->
                command.select("a").forEach { a ->
                    a.attr("href").also { a.attr("href", "${moduleName}/${it}") }
                }
                command.childNodes().toList().forEachIndexed { index, child ->
                    if (index == 0) {
                        child.attr("id", "$moduleName-nav-submenu")
                    }
                    node.appendChild(child)
                }
            }

            Files.write(output.resolve("navigation.html").toPath(), listOf(node.outerHtml()))
            node.select("a").forEach { a ->
                a.attr("href").also { a.attr("href", "../${it}") }
            }
            navigationFragments.forEach { (key, _) ->
                Files.write(
                    output.resolve(key).resolve("navigation.html").toPath(),
                    listOf(node.outerHtml())
                )
            }
        }
    }
}