package utils

import junit.framework.Assert.assertEquals
import org.jsoup.Jsoup

/**
 * Parses it using JSOUP, trims whitespace at the end of the line and asserts if they are equal
 * parsing is required to unify the formatting
 */
fun assertHtmlEqualsIgnoringWhitespace(expected: String, actual: String) {
    assertEquals(
        Jsoup.parse(expected).outerHtml().trimSpacesAtTheEndOfLine(),
        Jsoup.parse(actual).outerHtml().trimSpacesAtTheEndOfLine()
    )
}

private fun String.trimSpacesAtTheEndOfLine(): String =
    replace(" \n", "\n")