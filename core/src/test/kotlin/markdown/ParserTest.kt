package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka.toTestString
import org.jetbrains.dokka.parseMarkdown
import org.junit.Ignore

@Ignore public class ParserTest {
    fun runTestFor(text : String) {
        println("MD: ---")
        println(text)
        val markdownTree = parseMarkdown(text)
        println("AST: ---")
        println(markdownTree.toTestString())
        println()
    }

    @Test fun text() {
        runTestFor("text")
    }

    @Test fun textWithSpaces() {
        runTestFor("text and string")
    }

    @Test fun textWithColon() {
        runTestFor("text and string: cool!")
    }

    @Test fun link() {
        runTestFor("text [links]")
    }

    @Test fun linkWithHref() {
        runTestFor("text [links](http://google.com)")
    }

    @Test fun multiline() {
        runTestFor(
                """
text
and
string
""")
    }

    @Test fun para() {
        runTestFor(
                """
paragraph number
one

paragraph
number two
""")
    }

    @Test fun bulletList() {
        runTestFor(
                """* list item 1
* list item 2
""")
    }

    @Test fun bulletListWithLines() {
        runTestFor(
                """
* list item 1
  continue 1
* list item 2
  continue 2
 """)
    }

    @Test fun bulletListStrong() {
        runTestFor(
                """
* list *item* 1
  continue 1
* list *item* 2
  continue 2
 """)
    }

    @Test fun emph() {
        runTestFor("*text*")
    }

    @Test fun underscoresNoEmph() {
        runTestFor("text_with_underscores")
    }

    @Test fun emphUnderscores() {
        runTestFor("_text_")
    }

    @Test fun singleStar() {
        runTestFor("Embedded*Star")
    }

    @Test fun directive() {
        runTestFor("A text \${code with.another.value} with directive")
    }

    @Test fun emphAndEmptySection() {
        runTestFor("*text*\n\$sec:\n")
    }

    @Test fun emphAndSection() {
        runTestFor("*text*\n\$sec: some text\n")
    }

    @Test fun emphAndBracedSection() {
        runTestFor("Text *bold* text \n\${sec}: some text")
    }

    @Test fun section() {
        runTestFor(
                "Plain text \n\$one: Summary \n\${two}: Description with *emphasis* \n\${An example of a section}: Example")
    }

    @Test fun anonymousSection() {
        runTestFor("Summary\n\nDescription\n")
    }

    @Test fun specialSection() {
        runTestFor(
                "Plain text \n\$\$summary: Summary \n\${\$description}: Description \n\${\$An example of a section}: Example")
    }

    @Test fun emptySection() {
        runTestFor(
                "Plain text \n\$summary:")
    }

    val b = "$"
    @Test fun pair() {
        runTestFor(
                """Represents a generic pair of two values.

There is no meaning attached to values in this class, it can be used for any purpose.
Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.

An example of decomposing it into values:
${b}{code test.tuples.PairTest.pairMultiAssignment}

${b}constructor: Creates new instance of [Pair]
${b}first: First value
${b}second: Second value""""
                  )
    }

}

