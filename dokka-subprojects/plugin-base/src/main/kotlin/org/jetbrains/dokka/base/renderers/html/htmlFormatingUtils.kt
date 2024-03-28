/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.FlowContent
import kotlinx.html.span

public fun FlowContent.buildTextBreakableAfterCapitalLetters(name: String, hasLastElement: Boolean = false) {
    if (name.contains(" ")) {
        val withOutSpaces = name.split(" ")
        withOutSpaces.dropLast(1).forEach {
            buildBreakableText(it)
            +" "
        }
        buildBreakableText(withOutSpaces.last())
    } else {
        val content = name.replace(Regex("(?<=[a-z])([A-Z])"), " $1").split(" ")
        joinToHtml(content, hasLastElement) {
            it
        }
    }
}

/**
 * Makes [name] breakable by inserting `<wbr>` element after each occurrence of `.`.
 */
@Deprecated(
    "`buildBreakableCharSeparatedHtml` should be used instead with `.` as `breakableChar`",
    ReplaceWith("buildBreakableCharSeparatedHtml(name, '.')")
)
public fun FlowContent.buildBreakableDotSeparatedHtml(name: String) {
    buildBreakableCharSeparatedHtml(name, '.')
}

/**
 * Makes [name] breakable by inserting `<wbr>` element after each occurrence of [breakableChar].
 */
public fun FlowContent.buildBreakableCharSeparatedHtml(name: String, breakableChar: Char) {
    val phrases = name.split(breakableChar)
    phrases.forEachIndexed { i, e ->
        val elementWithOptionalChar = e.takeIf { i == phrases.lastIndex } ?: "$e$breakableChar"
        if (e.length > 10) {
            buildTextBreakableAfterCapitalLetters(elementWithOptionalChar, hasLastElement = i == phrases.lastIndex)
        } else {
            buildBreakableHtmlElement(elementWithOptionalChar, i == phrases.lastIndex)
        }
    }
}

private fun FlowContent.joinToHtml(elements: List<String>, hasLastElement: Boolean = true, onEach: (String) -> String) {
    elements.dropLast(1).forEach {
        buildBreakableHtmlElement(onEach(it))
    }
    elements.takeIf { it.isNotEmpty() && it.last().isNotEmpty() }?.let {
        if (hasLastElement) {
            span {
                buildBreakableHtmlElement(it.last(), last = true)
            }
        } else {
            buildBreakableHtmlElement(it.last(), last = false)
        }
    }
}

private fun FlowContent.buildBreakableHtmlElement(element: String, last: Boolean = false) {
    element.takeIf { it.isNotBlank() }?.let {
        span {
            +it
        }
    }
    if (!last) {
        wbr { }
    }
}

public fun FlowContent.buildBreakableText(name: String) {
    if (name.contains(".")) buildBreakableCharSeparatedHtml(name, '.')
    else if (name.contains("_")) buildBreakableCharSeparatedHtml(name, '_')
    else buildTextBreakableAfterCapitalLetters(name, hasLastElement = true)
}
