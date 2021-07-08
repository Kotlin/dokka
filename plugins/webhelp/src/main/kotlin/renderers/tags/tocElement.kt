package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

@HtmlTagMarker
inline fun FlowContent.tocElement(
    id: String,
    crossinline block: TOC_ELEMENT.() -> Unit = {}
): Unit = TOC_ELEMENT(mapOf("id" to id), consumer).visit(block)


open class TOC_ELEMENT(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("toc-element", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag