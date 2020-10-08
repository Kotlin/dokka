package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.toJsonString

@HtmlTagMarker
fun FlowOrPhrasingContent.wbr(classes: String? = null, block: WBR.() -> Unit = {}): Unit =
    WBR(attributesMapOf("class", classes), consumer).visit(block)

@Suppress("unused")
open class WBR(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("wbr", consumer, initialAttributes, namespace = null, inlineTag = true, emptyTag = false),
    HtmlBlockInlineTag

fun FlowOrPhrasingOrMetaDataContent.templateCommand(data: Command, block: TemplateCommand.() -> Unit = {}): Unit =
    TemplateCommand(attributesMapOf("data", toJsonString(data)), consumer).visit(block)

fun <T> TagConsumer<T>.templateCommand(data: Command, block: TemplateCommand.() -> Unit = {}): T =
    TemplateCommand(attributesMapOf("data", toJsonString(data)), this).visitAndFinalize(this, block)

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
