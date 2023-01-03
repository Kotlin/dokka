package org.jetbrains.dokka.html.templating

import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.html.renderers.TemplateBlock
import org.jetbrains.dokka.html.renderers.command.consumers.ImmediateResolutionTagConsumer

interface ImmediateHtmlCommandConsumer {
    fun canProcess(command: Command): Boolean

    fun <R> processCommand(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>)

    fun <R> processCommandAndFinalize(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>): R
}