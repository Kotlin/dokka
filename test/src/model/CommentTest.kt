package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class CommentTest {
    Test fun emptyDoc() {
        verifyModel("test/data/comments/emptyDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationContent.Empty, doc)
            }
        }
    }

    Test fun emptyDocButComment() {
        verifyModel("test/data/comments/emptyDocButComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationContent.Empty, doc)
            }
        }
    }

    Test fun multilineDoc() {
        verifyModel("test/data/comments/multilineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1".toRichString(), doc.summary)
                assertEquals("doc2\ndoc3".toRichString(), doc.description)
            }
        }
    }

    Test fun multipleDocs() {
        verifyModel("test/data/comments/multipleDocs.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1".toRichString(), doc.summary)
                assertEquals("doc2\ndoc3".toRichString(), doc.description)
            }
        }
    }

    Test fun multilineDocWithComment() {
        verifyModel("test/data/comments/multilineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1".toRichString(), doc.summary)
                assertEquals("doc2\ndoc3".toRichString(), doc.description)
            }
        }
    }

    Test fun oneLineDoc() {
        verifyModel("test/data/comments/oneLineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc".toRichString(), doc.summary)
            }
        }
    }

    Test fun oneLineDocWithComment() {
        verifyModel("test/data/comments/oneLineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc".toRichString(), doc.summary)
            }
        }
    }

    Test fun oneLineDocWithEmptyLine() {
        verifyModel("test/data/comments/oneLineDocWithEmptyLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc".toRichString(), doc.summary)
            }
        }
    }

    Test fun emptySection() {
        verifyModel("test/data/comments/emptySection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NormalStyle, NormalStyle)
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals(RichString.empty, text)
                }
            }
        }
    }

    Test fun section1() {
        verifyModel("test/data/comments/section1.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one".toRichString(), text)
                }
            }
        }
    }

    Test fun section2() {
        verifyModel("test/data/comments/section2.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(3, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one".toRichString(), text)
                }
                with (doc.sections["two"]!!) {
                    assertEquals("two", label)
                    assertEquals("section two".toRichString(), text)
                }
            }
        }
    }

    Test fun sectionOnOneLine() {
        verifyModel("test/data/comments/sectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("same line".toRichString(), text)
                }
            }
        }
    }

    Test fun emptySectionOnOneLine() {
        verifyModel("test/data/comments/emptySectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals(RichString.empty, text)
                }
            }
        }
    }

    Test fun multilineSection() {
        verifyModel("test/data/comments/multilineSection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("""line one
line two""".toRichString(), text)
                }
            }
        }
    }

    Test fun sectionWithBracedLabel() {
        verifyModel("test/data/comments/sectionWithBracedLabel.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary".toRichString(), doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections["this.label.is.really.long"]!!) {
                    assertEquals("this.label.is.really.long", label)
                    assertEquals("section one".toRichString(), text)
                }
            }
        }
    }
}
