/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.test.assertEquals

/**
 * Parses it using JSOUP, trims whitespace at the end of the line and asserts if they are equal
 * parsing is required to unify the formatting
 */
public fun assertHtmlEqualsIgnoringWhitespace(expected: String, actual: String) {
    val ignoreFormattingSettings = Document.OutputSettings().indentAmount(0).outline(true)
    assertEquals(
        Jsoup.parse(expected).outputSettings(ignoreFormattingSettings).outerHtml().trimSpacesAtTheEndOfLine(),
        Jsoup.parse(actual).outputSettings(ignoreFormattingSettings).outerHtml().trimSpacesAtTheEndOfLine()
    )
}

private fun String.trimSpacesAtTheEndOfLine(): String =
    replace(" \n", "\n")
