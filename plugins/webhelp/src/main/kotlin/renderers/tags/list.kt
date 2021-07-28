package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

@HtmlTagMarker
fun FlowOrPhrasingContent.list(classes: String? = null, block: LIST.() -> Unit = {}): Unit =
    LIST(attributesMapOf("class", classes), consumer).visit(block)

@HtmlTagMarker
inline fun LIST.li(classes : String? = null, crossinline block : LI.() -> Unit = {}) : Unit = LI(attributesMapOf("class", classes), consumer).visit(block)

open class LIST(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("list", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag