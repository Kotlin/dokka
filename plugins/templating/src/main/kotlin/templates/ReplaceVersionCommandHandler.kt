package templates

import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.CommandHandler
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File

class ReplaceVersionCommandHandler(private val context: DokkaContext) : CommandHandler {

    override fun canHandle(command: Command): Boolean = command is ReplaceVersionsCommand

    override fun handleCommand(element: Element, command: Command, input: File, output: File) {
        val position = element.elementSiblingIndex()
        val parent = element.parent()
        element.remove()
        context.configuration.moduleVersion?.takeIf { it.isNotEmpty() }
            ?.let { parent.insertChildren(position, TextNode(it)) }
    }
}