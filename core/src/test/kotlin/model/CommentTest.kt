package org.jetbrains.dokka.tests

import org.junit.Test
import org.junit.Assert.*
import org.jetbrains.dokka.*

public class CommentTest {

    @Test fun codeBlockComment() {
        verifyModel("testdata/comments/codeBlockComment.kt") { model ->
            with(model.members.single().members.first()) {
                assertEqualsIgnoringSeparators("""[code lang=brainfuck]
                                |
                                |++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.
                                |
                                |[/code]
                                |""".trimMargin(),
                        content.toTestString())
            }
            with(model.members.single().members.last()) {
                assertEqualsIgnoringSeparators("""[code]
                                |
                                |a + b - c
                                |
                                |[/code]
                                |""".trimMargin(),
                        content.toTestString())
            }
        }
    }

    @Test fun emptyDoc() {
        verifyModel("testdata/comments/emptyDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    @Test fun emptyDocButComment() {
        verifyModel("testdata/comments/emptyDocButComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    @Test fun multilineDoc() {
        verifyModel("testdata/comments/multilineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    @Test fun multilineDocWithComment() {
        verifyModel("testdata/comments/multilineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    @Test fun oneLineDoc() {
        verifyModel("testdata/comments/oneLineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun oneLineDocWithComment() {
        verifyModel("testdata/comments/oneLineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun oneLineDocWithEmptyLine() {
        verifyModel("testdata/comments/oneLineDocWithEmptyLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun emptySection() {
        verifyModel("testdata/comments/emptySection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(1, content.sections.count())
                with (content.findSectionByTag("one")!!) {
                    assertEquals("One", tag)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    @Test fun quotes() {
        verifyModel("testdata/comments/quotes.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("it's \"useful\"", content.summary.toTestString())
            }
        }
    }

    @Test fun section1() {
        verifyModel("testdata/comments/section1.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(1, content.sections.count())
                with (content.findSectionByTag("one")!!) {
                    assertEquals("One", tag)
                    assertEquals("section one", toTestString())
                }
            }
        }
    }

    @Test fun section2() {
        verifyModel("testdata/comments/section2.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.findSectionByTag("one")!!) {
                    assertEquals("One", tag)
                    assertEquals("section one", toTestString())
                }
                with (content.findSectionByTag("two")!!) {
                    assertEquals("Two", tag)
                    assertEquals("section two", toTestString())
                }
            }
        }
    }

    @Test fun multilineSection() {
        verifyModel("testdata/comments/multilineSection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(1, content.sections.count())
                with (content.findSectionByTag("one")!!) {
                    assertEquals("One", tag)
                    assertEquals("""line one
line two""", toTestString())
                }
            }
        }
    }

    @Test fun directive() {
        verifyModel("testdata/comments/directive.kt") { model ->
            with(model.members.single().members[3]) {
                assertEquals("Summary", content.summary.toTestString())
                with (content.description) {
                    assertEqualsIgnoringSeparators("""
                        |[code lang=kotlin]
                        |if (true) {
                        |    println(property)
                        |}
                        |[/code]
                        |[code lang=kotlin]
                        |if (true) {
                        |    println(property)
                        |}
                        |[/code]
                        |[code lang=kotlin]
                        |if (true) {
                        |    println(property)
                        |}
                        |[/code]
                        |[code lang=kotlin]
                        |if (true) {
                        |    println(property)
                        |}
                        |[/code]
                        |""".trimMargin(), toTestString())
                }
            }
        }
    }
}
