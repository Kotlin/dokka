package org.jetbrains.dokka.base.templating

import org.jetbrains.dokka.base.renderers.html.TemplateBlock
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.plugability.DokkaContext

interface ImmediateHtmlCommandConsumer {
    fun canProcess(command:Command): Boolean

    fun <R> processCommand(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>)

    fun <R> processCommandAndFinalize(command: Command, block: TemplateBlock, tagConsumer: ImmediateResolutionTagConsumer<R>): R
}

