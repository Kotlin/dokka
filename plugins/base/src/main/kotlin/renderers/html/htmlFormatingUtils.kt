package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.FlowContent
import kotlinx.html.span

fun FlowContent.buildTextBreakableAfterCapitalLetters(name: String, hasLastElement: Boolean = false) {
    if (name.contains(" ")) {
        val withOutSpaces = name.split(" ")
        withOutSpaces.dropLast(1).forEach {
            buildBreakableText(it)
            +" "
        }
        buildBreakableText(withOutSpaces.last())
    } else {
        val content = name.replace(Regex("(?!^)([A-Z])"), " $1").split(" ")
        joinToHtml(content, hasLastElement) {
            it
        }
    }
}

fun FlowContent.buildBreakableDotSeparatedHtml(name: String) {
    val phrases = name.split(".")
    phrases.forEachIndexed { i, e ->
        val elementWithOptionalDot = e.takeIf { i == phrases.lastIndex } ?: "$e."
        if (e.length > 10) {
            buildTextBreakableAfterCapitalLetters(elementWithOptionalDot, hasLastElement = i == phrases.lastIndex)
        } else {
            buildBreakableHtmlElement(elementWithOptionalDot, i == phrases.lastIndex)
        }
    }
}

private fun FlowContent.joinToHtml(elements: List<String>, onEach: (String) -> String) {
    elements.dropLast(1).forEach {
        buildBreakableHtmlElement(onEach(it))
    }
    span {
        buildBreakableHtmlElement(elements.last(), last = true)
    }
}

private fun FlowContent.buildBreakableHtmlElement(element: String, last: Boolean = false) {
    span {
        +element
    }
    if (!last) {
        wbr { }
    }
}

fun FlowContent.buildBreakableText(name: String) =
    if (name.contains(".")) buildBreakableDotSeparatedHtml(name)
    else buildTextBreakableAfterCapitalLetters(name)