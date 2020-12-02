package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.base.templating.Command
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class AddToNavigationCommandHandler : CommandHandler {
    private val navigationFragments = ConcurrentHashMap<String, Element>()

    override fun handleCommand(element: Element, command: Command, input: File, output: File) {
        command as AddToNavigationCommand
        navigationFragments[command.moduleName] = element
    }

    override fun canHandle(command: Command) = command is AddToNavigationCommand

    override fun finish(output: File) {
        if (navigationFragments.isNotEmpty()) {
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

            node.select("a").forEach { a ->
                a.attr("href")?.also { a.attr("href", "../${it}") }
            }
            navigationFragments.keys.forEach {
                Files.write(
                    output.resolve(it).resolve("navigation.html").toPath(),
                    listOf(node.outerHtml())
                )
            }
        }
    }
}