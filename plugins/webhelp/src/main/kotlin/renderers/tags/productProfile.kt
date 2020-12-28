package org.jetbrains.dokka.webhelp.renderers.tags

import kotlinx.html.*

@HtmlTagMarker
inline fun FlowContent.productProfile(
    id: String,
    name: String,
    startPage: String,
    crossinline block: PRODUCT_PROFILE.() -> Unit = {}
): Unit = PRODUCT_PROFILE(mapOf("id" to id, "name" to name, "startPage" to startPage), consumer).visit(block)

@HtmlTagMarker
inline fun <T, C : TagConsumer<T>> C.productProfile(
    id: String,
    name: String,
    startPage: String,
    crossinline block: PRODUCT_PROFILE.() -> Unit = {}
): T =
    PRODUCT_PROFILE(mapOf("id" to id, "name" to name, "startPage" to startPage), this).visitAndFinalize(this, block)


open class PRODUCT_PROFILE(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag("product-profile", consumer, initialAttributes, namespace = null, inlineTag = false, emptyTag = false),
    HtmlBlockInlineTag