/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.base.templating.Command
import org.jetbrains.dokka.base.templating.toJsonString

public typealias TemplateBlock = TemplateCommand.() -> Unit

@HtmlTagMarker
public fun FlowOrPhrasingContent.wbr(classes: String? = null, block: WBR.() -> Unit = {}): Unit =
    WBR(attributesMapOf("class", classes), consumer).visit(block)

@Suppress("unused")
public open class WBR(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("wbr", consumer, initialAttributes, namespace = null, inlineTag = true, emptyTag = false),
    HtmlBlockInlineTag

/**
 * Work-around until next version of kotlinx.html doesn't come out
 */
@HtmlTagMarker
public inline fun FlowOrPhrasingContent.strike(classes : String? = null, crossinline block : STRIKE.() -> Unit = {}) : Unit = STRIKE(attributesMapOf("class", classes), consumer).visit(block)

public open class STRIKE(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) :
    HTMLTag("strike", consumer, initialAttributes, null, false, false), HtmlBlockInlineTag

@HtmlTagMarker
public inline fun FlowOrPhrasingContent.underline(classes : String? = null, crossinline block : UNDERLINE.() -> Unit = {}) : Unit = UNDERLINE(attributesMapOf("class", classes), consumer).visit(block)

public open class UNDERLINE(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) :
    HTMLTag("u", consumer, initialAttributes, null, false, false), HtmlBlockInlineTag

public const val TEMPLATE_COMMAND_SEPARATOR: String = ":"
public const val TEMPLATE_COMMAND_BEGIN_BORDER: String = "[+]cmd"
public const val TEMPLATE_COMMAND_END_BORDER: String = "[-]cmd"

public fun FlowOrMetaDataContent.templateCommandAsHtmlComment(data: Command, block: FlowOrMetaDataContent.() -> Unit = {}): Unit =
    (consumer as? ImmediateResolutionTagConsumer)?.processCommand(data, block)
        ?:  let{
            comment( "$TEMPLATE_COMMAND_BEGIN_BORDER$TEMPLATE_COMMAND_SEPARATOR${toJsonString(data)}")
            block()
            comment(TEMPLATE_COMMAND_END_BORDER)
        }

public fun <T: Appendable> T.templateCommandAsHtmlComment(command: Command, action: T.() -> Unit ) {
    append("<!--$TEMPLATE_COMMAND_BEGIN_BORDER$TEMPLATE_COMMAND_SEPARATOR${toJsonString(command)}-->")
    action()
    append("<!--$TEMPLATE_COMMAND_END_BORDER-->")
}

public fun FlowOrMetaDataContent.templateCommand(data: Command, block: TemplateBlock = {}): Unit =
    (consumer as? ImmediateResolutionTagConsumer)?.processCommand(data, block)
        ?: TemplateCommand(attributesMapOf("data", toJsonString(data)), consumer).visit(block)

public fun <T> TagConsumer<T>.templateCommand(data: Command, block: TemplateBlock = {}): T =
    (this as? ImmediateResolutionTagConsumer)?.processCommandAndFinalize(data, block)
        ?: TemplateCommand(attributesMapOf("data", toJsonString(data)), this).visitAndFinalize(this, block)

public fun templateCommandFor(data: Command, consumer: TagConsumer<*>): TemplateCommand =
    TemplateCommand(attributesMapOf("data", toJsonString(data)), consumer)

public class TemplateCommand(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
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
public fun TemplateBlock.buildAsInnerHtml(): String = createHTML(prettyPrint = false).run {
    TemplateCommand(emptyMap, this).visitAndFinalize(this, this@buildAsInnerHtml).substringAfter(">").substringBeforeLast("<")
}
