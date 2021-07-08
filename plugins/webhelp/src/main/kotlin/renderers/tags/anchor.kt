package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

open class ANCHOR(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("anchor", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag

@HtmlTagMarker
inline fun FlowContent.anchor(name: String, crossinline block: ANCHOR.() -> Unit = {}): Unit =
    ANCHOR(attributesMapOf("name", name), consumer).visit(block)
