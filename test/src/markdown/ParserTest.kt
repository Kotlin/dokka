package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka.*

public class ParserTest {
    Test fun text() {
        val markdown = MarkdownProcessor().parse("text")
        println(markdown.dump())
    }

    Test fun textWithSpaces() {
        val markdown = MarkdownProcessor().parse("text and string")
        println(markdown.dump())
    }

    Test fun multiline() {
        val markdown = MarkdownProcessor().parse(
"""
text
and
string
""")
        println(markdown.dump())
    }

    Test fun para() {
        val markdown = MarkdownProcessor().parse(
"""paragraph number
one

paragraph
number two
""")
        println(markdown.dump())
    }

    Test fun bulletList() {
        val markdown = MarkdownProcessor().parse(
"""
* list item 1
* list item 2
""")
        println(markdown.dump())
    }

    Test fun emph() {
        val markdown = MarkdownProcessor().parse("*text*")
        println(markdown.dump())
    }
}

