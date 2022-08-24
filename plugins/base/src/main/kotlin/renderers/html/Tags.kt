package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.toJsonString

typealias TemplateBlock = TemplateCommand.() -> Unit

@HtmlTagMarker
fun FlowOrPhrasingContent.wbr(classes: String? = null, block: WBR.() -> Unit = {}): Unit =
    WBR(attributesMapOf("class", classes), consumer).visit(block)

@Suppress("unused")
open class WBR(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("wbr", consumer, initialAttributes, namespace = null, inlineTag = true, emptyTag = false),
    HtmlBlockInlineTag

/**
 * Work-around until next version of kotlinx.html doesn't come out
 */
@HtmlTagMarker
inline fun FlowOrPhrasingContent.strike(classes : String? = null, crossinline block : STRIKE.() -> Unit = {}) : Unit = STRIKE(attributesMapOf("class", classes), consumer).visit(block)

open class STRIKE(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) :
    HTMLTag("strike", consumer, initialAttributes, null, false, false), HtmlBlockInlineTag

@HtmlTagMarker
inline fun FlowOrPhrasingContent.underline(classes : String? = null, crossinline block : UNDERLINE.() -> Unit = {}) : Unit = UNDERLINE(attributesMapOf("class", classes), consumer).visit(block)

open class UNDERLINE(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) :
    HTMLTag("u", consumer, initialAttributes, null, false, false), HtmlBlockInlineTag

const val TEMPLATE_COMMAND_SEPARATOR = ":"
const val TEMPLATE_COMMAND_BEGIN_BORDER  = "[+]cmd"
const val TEMPLATE_COMMAND_END_BORDER  = "[-]cmd"

fun FlowOrMetaDataContent.templateCommandAsHtmlComment(data: Command, block: FlowOrMetaDataContent.() -> Unit = {}): Unit =
    (consumer as? ImmediateResolutionTagConsumer)?.processCommand(data, block)
        ?:  let{
            comment( "$TEMPLATE_COMMAND_BEGIN_BORDER$TEMPLATE_COMMAND_SEPARATOR${toJsonString(data)}")
            block()
            comment(TEMPLATE_COMMAND_END_BORDER)
        }

fun <T: Appendable> T.templateCommandAsHtmlComment(command: Command, action: T.() -> Unit ) {
    append("<!--$TEMPLATE_COMMAND_BEGIN_BORDER$TEMPLATE_COMMAND_SEPARATOR${toJsonString(command)}-->")
    action()
    append("<!--$TEMPLATE_COMMAND_END_BORDER-->")
}

fun FlowOrMetaDataContent.templateCommand(data: Command, block: TemplateBlock = {}): Unit =
    (consumer as? ImmediateResolutionTagConsumer)?.processCommand(data, block)
        ?: TemplateCommand(attributesMapOf("data", toJsonString(data)), consumer).visit(block)

fun <T> TagConsumer<T>.templateCommand(data: Command, block: TemplateBlock = {}): T =
    (this as? ImmediateResolutionTagConsumer)?.processCommandAndFinalize(data, block)
        ?: TemplateCommand(attributesMapOf("data", toJsonString(data)), this).visitAndFinalize(this, block)

fun templateCommandFor(data: Command, consumer: TagConsumer<*>) =
    TemplateCommand(attributesMapOf("data", toJsonString(data)), consumer)

class TemplateCommand(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag(
        "dokka-template-command",
        consumer,
        initialAttributes,
        namespace = null,
        inlineTag = true,
        emptyTag = false
    ),
    CommonAttributeGroupFacadeFlowInteractivePhrasingContent

// This hack is outrageous. I hate it but I cannot find any other way around `kotlinx.html` type system.
fun TemplateBlock.buildAsInnerHtml(): String = createHTML(prettyPrint = false).run {
    TemplateCommand(emptyMap, this).visitAndFinalize(this, this@buildAsInnerHtml).substringAfter(">").substringBeforeLast("<")
}
