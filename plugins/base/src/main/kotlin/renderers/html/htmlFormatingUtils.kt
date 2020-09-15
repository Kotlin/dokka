package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.FlowContent
import kotlinx.html.span

fun FlowContent.buildTextBreakableAfterCapitalLetters(name: String) {
    if (name.contains(" ")) {
        val withOutSpaces = name.split(" ")
        withOutSpaces.dropLast(1).forEach {
            buildBreakableText(it)
            +" "
        }
        buildBreakableText(withOutSpaces.last())
    } else {
        val content = name.replace(Regex("(?!^)([A-Z])"), " $1").split(" ")
        joinToHtml(content){
            it
        }
    }
}

fun FlowContent.buildBreakableDotSeparatedHtml(name: String) {
    val phrases = name.split(".")
    joinToHtml(phrases){
        "$it."
    }
}

private fun FlowContent.joinToHtml(elements: List<String>, onEach: (String) -> String) {
    elements.dropLast(1).forEach {
        span {
            +onEach(it)
        }
        wbr { }
    }
    span {
        +elements.last()
    }
}

fun FlowContent.buildBreakableText(name: String) =
    if (name.contains(".")) buildBreakableDotSeparatedHtml(name)
    else buildTextBreakableAfterCapitalLetters(name)