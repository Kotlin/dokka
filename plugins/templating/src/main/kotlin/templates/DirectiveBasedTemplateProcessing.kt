package org.jetbrains.dokka.templates

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.renderers.html.TEMPLATE_COMMAND_BEGIN_BORDER
import org.jetbrains.dokka.base.renderers.html.TEMPLATE_COMMAND_END_BORDER
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.File
import java.nio.file.Files

class DirectiveBasedHtmlTemplateProcessingStrategy(private val context: DokkaContext) : TemplateProcessingStrategy {

    private val directiveBasedCommandHandlers =
        context.plugin<TemplatingPlugin>().query { directiveBasedCommandHandlers }

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean =
        if (input.isFile && input.extension == "html") {
            val document = Jsoup.parse(input, "UTF-8")
            document.outputSettings().indentAmount(0).prettyPrint(false)

            document.select("dokka-template-command").forEach {
                handleCommand(it, parseJson(it.attr("data")), input, output)
            }
            extractCommandsFromComments(document) { nodes, command ->
                val nodesTrimed =
                    nodes.dropWhile { (it is TextNode && it.isBlank).also { res -> if (res) it.remove() } }
                        .dropLastWhile { (it is TextNode && it.isBlank).also { res -> if (res) it.remove() } }
                handleCommand(nodesTrimed, command, input, output)
            }

            Files.write(output.toPath(), listOf(document.outerHtml()))
            true
        } else false

    fun handleCommand(element: Element, command: Command, input: File, output: File) {
        val handlers = directiveBasedCommandHandlers.filter { it.canHandle(command) }
        if (handlers.isEmpty())
            context.logger.warn("Unknown templating command $command")
        else
            handlers.forEach { it.handleCommand(element, command, input, output) }
    }

    fun handleCommand(nodes: List<Node>, command: Command, input: File, output: File) {
        val handlers = directiveBasedCommandHandlers.filterIsInstance<CommentCommandHandler>().filter { it.canHandle(command) }
        if (handlers.isEmpty())
            context.logger.warn("Unknown templating command $command")
        else
            handlers.forEach { it.handleCommand(nodes, command, input, output) }
    }

    private fun extractCommandsFromComments(node: Node, startFrom: Int = 0, handler: (List<Node>, Command) -> Unit) {
        val nodes: MutableList<Node> = mutableListOf()
        var lastStartBorder: Comment? = null
        var firstStartBorder: Comment? = null
        for (ind in startFrom until node.childNodeSize()) {
            when (val currentChild = node.childNode(ind)) {
                is Comment -> if (currentChild.data?.startsWith(TEMPLATE_COMMAND_BEGIN_BORDER) == true) {
                    lastStartBorder = currentChild
                    firstStartBorder = firstStartBorder ?: currentChild
                    nodes.clear()
                } else if (lastStartBorder != null && currentChild.data?.startsWith(TEMPLATE_COMMAND_END_BORDER) == true) {
                    lastStartBorder.remove()
                    val cmd: Command? =
                        lastStartBorder.data?.removePrefix(TEMPLATE_COMMAND_BEGIN_BORDER)?.let { parseJson(it) }
                    cmd?.let { handler(nodes, it) }
                    currentChild.remove()
                    extractCommandsFromComments(node, firstStartBorder?.siblingIndex() ?: 0, handler)
                    return
                } else {
                    lastStartBorder?.let { nodes.add(currentChild) }
                }
                else -> {
                    extractCommandsFromComments(currentChild, handler = handler)
                    lastStartBorder?.let { nodes.add(currentChild) }
                }
            }
        }
    }

    override fun finish(output: File) {
        directiveBasedCommandHandlers.forEach { it.finish(output) }
    }
}
