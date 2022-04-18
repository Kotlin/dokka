package org.jetbrains.dokka.versioning


import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.templates.CommandHandler
import org.jsoup.nodes.Element
import java.io.File

class ReplaceVersionCommandHandler(context: DokkaContext) : CommandHandler {

    val versionsNavigationCreator by lazy {
        context.plugin<VersioningPlugin>().querySingle { versionsNavigationCreator }
    }

    override fun canHandle(command: Command): Boolean = command is ReplaceVersionsCommand

    override fun handleCommandAsTag(command: Command, body: Element, input: File, output: File) {
        body.empty()
        body.append(versionsNavigationCreator(output))
    }
}