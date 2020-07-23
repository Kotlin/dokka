package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.FlowContent
import kotlinx.html.span

fun FlowContent.buildBreakableDotSeparatedHtml(name: String) {
    val phrases = name.split(".")
    phrases.dropLast(1).forEach {
        span {
            +"$it."
        }
        wbr { }
    }
    span {
        +phrases.last()
    }
}