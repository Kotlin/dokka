package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka
import org.jetbrains.dokka.MarkdownProcessor
import org.jetbrains.dokka.toTestString
import org.jetbrains.dokka.toHtml

public class ParserTest {
    fun runTestFor(text : String) {
        val markdownTree = MarkdownProcessor.parse(text)
        println(markdownTree.toTestString())
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

    Test fun emphAndEmptySection() {
        runTestFor("*text* \$sec:")
    }

    Test fun emphAndSection() {
        runTestFor("*text* \$sec: some text")
    }

    Test fun emphAndBracedSection() {
        runTestFor("Text *bold* text \${sec}: some text")
    }

    Test fun section() {
        runTestFor(
                "Plain text \$one: Summary \${two}: Description with *emphasis* \${An example of a section}: Example")
    }

    Test fun anonymousSection() {
        runTestFor("Summary\n\nDescription\n")
    }

    Test fun specialSection() {
        runTestFor(
                "Plain text \$\$summary: Summary \${\$description}: Description \${\$An example of a section}: Example")
    }

    Test fun emptySection() {
        runTestFor(
                "Plain text \$summary:")
    }

}

