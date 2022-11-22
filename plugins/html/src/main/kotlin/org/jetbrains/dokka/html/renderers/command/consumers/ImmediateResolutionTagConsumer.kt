package org.jetbrains.dokka.html.renderers.command.consumers

import kotlinx.html.TagConsumer
import kotlinx.html.visit
import org.jetbrains.dokka.html.renderers.TemplateBlock
import org.jetbrains.dokka.html.renderers.templateCommand
import org.jetbrains.dokka.html.renderers.templateCommandFor
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.html.DokkaHtml
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query

class ImmediateResolutionTagConsumer<out R>(
    private val downstream: TagConsumer<R>,
    private val context: DokkaContext
): TagConsumer<R> by downstream {
    fun processCommand(command: Command, block: TemplateBlock) {
        context.plugin<DokkaHtml>().query { immediateHtmlCommandConsumer }
            .find { it.canProcess(command) }
            ?.processCommand(command, block, this)
            ?: run { templateCommandFor(command, downstream).visit(block) }
    }

    fun processCommandAndFinalize(command: Command, block: TemplateBlock): R =
        context.plugin<DokkaHtml>().query { immediateHtmlCommandConsumer }
            .find { it.canProcess(command) }
            ?.processCommandAndFinalize(command, block, this)
            ?: downstream.templateCommand(command, block)
}

