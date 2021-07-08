package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*
import org.jetbrains.dokka.webhelp.renderers.tags.TOPIC.Companion.staticAttributes

@HtmlTagMarker
inline fun <T, C : TagConsumer<T>> C.topic(title: String, id: String, crossinline block: TOPIC.() -> Unit = {}): T =
    TOPIC(listOf("title" to title, "id" to id).toMap() + staticAttributes, this).visitAndFinalize(this, block)

@HtmlTagMarker
fun FlowOrPhrasingContent.topic(title: String, id: String, block: TOPIC.() -> Unit = {}): Unit =
    TOPIC(listOf("title" to title, "id" to id).toMap(), consumer).visit(block)

open class TOPIC(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag(
        "topic", consumer, initialAttributes,
        namespace = null, inlineTag = false, emptyTag = false
    ),
    HtmlBlockInlineTag {
    companion object {
        val staticAttributes = listOf(
            "xsi:noNamespaceSchemaLocation" to "https://resources.jetbrains.com/stardust/topic.v2.xsd",
            "xmlns:xsi" to "http://www.w3.org/2001/XMLSchema-instance"
        ).toMap()
    }
}