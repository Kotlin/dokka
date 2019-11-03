package org.jetbrains.dokka.tests.markdownParser

import org.jetbrains.dokka.pages.MarkdownToContentConverter
import org.jetbrains.dokka.parseMarkdown
import org.junit.Assert.assertTrue
import org.junit.Test


class MarkdownParserTest {

    @Test fun basicTest() {
        val markdown = """
            # Header 1 test
            this is some text
            more text
            let's say there are some parentheses, like ( and )
        """.trimIndent()
        val node = parseMarkdown(markdown)
        val content = MarkdownToContentConverter().buildContent(node, emptyList())
        assertTrue(content.isNotEmpty())
    }

}

