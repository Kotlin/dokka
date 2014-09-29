package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class CommentTest {
    Test fun emptyDoc() {
        verifyModel("test/data/comments/emptyDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, doc)
            }
        }
    }

    Test fun emptyDocButComment() {
        verifyModel("test/data/comments/emptyDocButComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, doc)
            }
        }
    }

    Test fun multilineDoc() {
        verifyModel("test/data/comments/multilineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", doc.summary.toTestString())
                assertEquals("doc2\ndoc3\n", doc.description.toTestString())
            }
        }
    }

    Test fun multipleDocs() {
        verifyModel("test/data/comments/multipleDocs.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", doc.summary.toTestString())
                assertEquals("doc2\ndoc3", doc.description.toTestString())
            }
        }
    }

    Test fun multilineDocWithComment() {
        verifyModel("test/data/comments/multilineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", doc.summary.toTestString())
                assertEquals("doc2\ndoc3", doc.description.toTestString())
            }
        }
    }

    Test fun oneLineDoc() {
        verifyModel("test/data/comments/oneLineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", doc.summary.toTestString())
            }
        }
    }

    Test fun oneLineDocWithComment() {
        verifyModel("test/data/comments/oneLineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", doc.summary.toTestString())
            }
        }
    }

    Test fun oneLineDocWithEmptyLine() {
        verifyModel("test/data/comments/oneLineDocWithEmptyLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", doc.summary.toTestString())
            }
        }
    }

    Test fun emptySection() {
        verifyModel("test/data/comments/emptySection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NormalStyle, NormalStyle)
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    Test fun explicitSummary() {
        verifyModel("test/data/comments/explicitSummary.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals("Description", doc.description.toTestString())
                assertEquals(2, doc.sections.count())
            }
        }
    }

    Test fun section1() {
        verifyModel("test/data/comments/section1.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one", toTestString())
                }
            }
        }
    }

    Test fun section2() {
        verifyModel("test/data/comments/section2.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(3, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one", toTestString())
                }
                with (doc.sections["two"]!!) {
                    assertEquals("two", label)
                    assertEquals("section two", toTestString())
                }
            }
        }
    }

    Test fun sectionOnOneLine() {
        verifyModel("test/data/comments/sectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("same line", toTestString())
                }
            }
        }
    }

    Test fun emptySectionOnOneLine() {
        verifyModel("test/data/comments/emptySectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    Test fun multilineSection() {
        verifyModel("test/data/comments/multilineSection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("""line one
line two""", toTestString())
                }
            }
        }
    }

    Test fun sectionWithBracedLabel() {
        verifyModel("test/data/comments/sectionWithBracedLabel.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", doc.summary.toTestString())
                assertEquals(2, doc.sections.count())
                with (doc.sections["this.label.is.really.long"]!!) {
                    assertEquals("this.label.is.really.long", label)
                    assertEquals("section one", toTestString())
                }
            }
        }
    }
}
