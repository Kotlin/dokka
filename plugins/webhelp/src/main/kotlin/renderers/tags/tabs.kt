package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

open class TABS(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("tabs", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag

@HtmlTagMarker
inline fun FlowContent.tabs(crossinline block: TABS.() -> Unit = {}): Unit =
    TABS(attributesMapOf(), consumer).visit(block)

open class TAB(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("tab", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag

@HtmlTagMarker
inline fun FlowContent.tab(
    title: String,
    style: String? = null,
    hideFromStructure: Boolean = true,
    crossinline block: TAB.() -> Unit = {}
): Unit =
    TAB(
        attributesMapOf("title", title, "style", style, "hide-from-structure", hideFromStructure.toString()),
        consumer
    ).visit(block)
