package org.jetbrains.dokka.versioning


import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.CommandHandler
import org.jsoup.nodes.Element
import java.io.File

object ReplaceVersionsCommand : Command

class ReplaceVersionCommandHandler(context: DokkaContext) : CommandHandler {

    val versionsNavigationCreator by lazy {
        context.plugin<VersioningPlugin>().querySingle { versionsNavigationCreator }
    }

    override fun canHandle(command: Command): Boolean = command is ReplaceVersionsCommand

    override fun handleCommand(element: Element, command: Command, input: File, output: File) {
        element.empty()
        element.append(versionsNavigationCreator(output))
    }
}