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

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        val position = body.elementSiblingIndex()
        val parent = body.parent()
        body.remove()
        context.configuration.moduleVersion?.takeIf { it.isNotEmpty() }
            ?.let { parent.insertChildren(position, TextNode(it)) }
    }
}