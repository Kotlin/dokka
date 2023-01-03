package org.jetbrains.dokka.html.renderers.command.consumers

import org.jetbrains.dokka.html.renderers.TemplateBlock
import org.jetbrains.dokka.html.renderers.buildAsInnerHtml
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.html.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand

object PathToRootConsumer: ImmediateHtmlCommandConsumer {
    override fun canProcess(command: Command) = command is PathToRootSubstitutionCommand

    override fun <R> processCommand(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>) {
        command as PathToRootSubstitutionCommand
        tagConsumer.onTagContentUnsafe { +block.buildAsInnerHtml().replace(command.pattern, command.default) }
    }

    override fun <R> processCommandAndFinalize(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>): R {
        processCommand(command, block, tagConsumer)
        return tagConsumer.finalize()
    }

}