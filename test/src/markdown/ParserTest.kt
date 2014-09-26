package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka
import org.jetbrains.dokka.MarkdownProcessor
import org.jetbrains.dokka.dump
import org.jetbrains.dokka.toHtml

public class ParserTest {
    fun runTestFor(text : String) {
        val markdownTree = MarkdownProcessor().parse(text)
        println(markdownTree.dump())
        println(markdownTree.toHtml())
    }

    Test fun text() {
        runTestFor("text")
    }

    Test fun textWithSpaces() {
        runTestFor("text and string")
    }

    Test fun link() {
        runTestFor("text [links]")
    }

    Test fun linkWithHref() {
        runTestFor("text [links](http://destination)")
    }

    Test fun multiline() {
        runTestFor(
                """
text
and
string
""")
    }

    Test fun para() {
        runTestFor(
                """
paragraph number
one

paragraph
number two
""")
    }

    Test fun bulletList() {
        runTestFor(
                """* list item 1
* list item 2
""")
    }

    Test fun bulletListWithLines() {
        runTestFor(
                """
* list item 1
  continue 1
* list item 2
  continue 2
 """)
    }

    Test fun bulletListStrong() {
        runTestFor(
                """
* list *item* 1
  continue 1
* list *item* 2
  continue 2
 """)
    }

    Test fun emph() {
        runTestFor("*text*")
    }
}

