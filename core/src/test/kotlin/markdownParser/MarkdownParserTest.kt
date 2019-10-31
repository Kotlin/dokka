package org.jetbrains.dokka.tests.markdownParser

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.buildContent
import org.jetbrains.dokka.parseMarkdown
import org.jetbrains.dokka.resolvers.ExternalLocationProvider
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
        val content = buildContent(node, emptyList())
        assertTrue(content.isNotEmpty())
    }

}

