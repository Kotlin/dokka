package org.jetbrains.dokka.base.renderers.html.command.consumers

import org.jetbrains.dokka.base.renderers.html.TemplateBlock
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.plugability.DokkaContext

class ReplaceVersionsConsumer(private val context: DokkaContext) : ImmediateHtmlCommandConsumer {
    override fun canProcess(command: Command) = command is ReplaceVersionsCommand

    override fun <R> processCommand(
        command: Command,
        block: TemplateBlock,
        tagConsumer: ImmediateResolutionTagConsumer<R>
    ) {
        command as ReplaceVersionsCommand
        tagConsumer.onTagContentUnsafe { +context.configuration.moduleVersion.orEmpty() }
    }

    override fun <R> processCommandAndFinalize(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>): R {
        processCommand(command, block, tagConsumer)
        return tagConsumer.finalize()
    }
}