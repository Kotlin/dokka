package org.jetbrains.dokka.base.renderers.html.command.consumers

import kotlinx.html.SPAN
import kotlinx.html.span
import kotlinx.html.unsafe
import kotlinx.html.visit
import org.jetbrains.dokka.base.renderers.html.TemplateBlock
import org.jetbrains.dokka.base.renderers.html.buildAsInnerHtml
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.templating.ResolveLinkCommand
import org.jetbrains.dokka.utilities.htmlEscape

object ResolveLinkConsumer: ImmediateHtmlCommandConsumer {
    override fun canProcess(command: Command) = command is ResolveLinkCommand

    override fun <R> processCommand(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>) {
        command as ResolveLinkCommand
        SPAN(mapOf("data-unresolved-link" to command.dri.toString().htmlEscape()), tagConsumer).visit {
            unsafe { block.buildAsInnerHtml() }
        }
    }

    override fun <R> processCommandAndFinalize(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>): R {
        command as ResolveLinkCommand
        return tagConsumer.span {
            attributes["data-unresolved-link"] = command.dri.toString().htmlEscape()
        }
    }
}