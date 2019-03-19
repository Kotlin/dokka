package org.jetbrains.dokka.tests

import org.junit.Test
import org.junit.Assert.*
import org.jetbrains.dokka.*

abstract class BaseCommentTest(val analysisPlatform: Platform) {
    val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun codeBlockComment() {
        checkSourceExistsAndVerifyModel("testdata/comments/codeBlockComment.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/comments/emptyDoc.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    @Test fun emptyDocButComment() {
        checkSourceExistsAndVerifyModel("testdata/comments/emptyDocButComment.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    @Test fun multilineDoc() {
        checkSourceExistsAndVerifyModel("testdata/comments/multilineDoc.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    @Test fun multilineDocWithComment() {
        checkSourceExistsAndVerifyModel("testdata/comments/multilineDocWithComment.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    @Test fun oneLineDoc() {
        checkSourceExistsAndVerifyModel("testdata/comments/oneLineDoc.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun oneLineDocWithComment() {
        checkSourceExistsAndVerifyModel("testdata/comments/oneLineDocWithComment.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun oneLineDocWithEmptyLine() {
        checkSourceExistsAndVerifyModel("testdata/comments/oneLineDocWithEmptyLine.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    @Test fun emptySection() {
        checkSourceExistsAndVerifyModel("testdata/comments/emptySection.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/comments/quotes.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("it's \"useful\"", content.summary.toTestString())
            }
        }
    }

    @Test fun section1() {
        checkSourceExistsAndVerifyModel("testdata/comments/section1.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/comments/section2.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/comments/multilineSection.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/comments/directive.kt", defaultModelConfig) { model ->
            with(model.members.single().members.first()) {
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

class JSCommentTest: BaseCommentTest(Platform.js)
class JVMCommentTest: BaseCommentTest(Platform.jvm)
class CommonCommentTest: BaseCommentTest(Platform.common)