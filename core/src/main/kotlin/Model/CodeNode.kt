package org.jetbrains.dokka.Model

import org.jsoup.helper.StringUtil.isWhitespace
import org.jsoup.nodes.TextNode

class CodeNode(text: String, baseUri: String): TextNode(text, baseUri) {

    override fun text(): String {
        return normaliseInitialWhitespace(wholeText.removePrefix("<code>")
            .removeSuffix("</code>"))
    }

    private fun normaliseInitialWhitespace(text: String): String {
        val sb = StringBuilder(text.length)
        removeInitialWhitespace(sb, text)
        return sb.toString()
    }

    /**
     * Remove initial whitespace.
     * @param accum builder to append to
     * @param string string to remove the initial whitespace
     */
    private fun removeInitialWhitespace(accum: StringBuilder, string: String) {
        var reachedNonWhite = false

        val len = string.length
        var c: Int
        var i = 0
        while (i < len) {
            c = string.codePointAt(i)
            if (isWhitespace(c) && !reachedNonWhite) {
                i += Character.charCount(c)
                continue
            } else {
                accum.appendCodePoint(c)
                reachedNonWhite = true
            }
            i += Character.charCount(c)
        }
    }
}