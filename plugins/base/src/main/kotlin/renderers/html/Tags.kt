package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*

@HtmlTagMarker
fun FlowOrPhrasingContent.wbr(classes : String? = null, block : WBR.() -> Unit = {}) : Unit = WBR(attributesMapOf("class", classes), consumer).visit(block)

@Suppress("unused")
open class WBR(initialAttributes : Map<String, String>, override val consumer : TagConsumer<*>) : HTMLTag("wbr", consumer, initialAttributes, null, true, false),
    HtmlBlockInlineTag {

}