package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class CommentTest {
    Test fun emptyDoc() {
        verifyModel("test/data/comments/emptyDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    Test fun emptyDocButComment() {
        verifyModel("test/data/comments/emptyDocButComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(Content.Empty, content)
            }
        }
    }

    Test fun multilineDoc() {
        verifyModel("test/data/comments/multilineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    Test fun multipleDocs() {
        verifyModel("test/data/comments/multipleDocs.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    Test fun multilineDocWithComment() {
        verifyModel("test/data/comments/multilineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc1", content.summary.toTestString())
                assertEquals("doc2\ndoc3", content.description.toTestString())
            }
        }
    }

    Test fun oneLineDoc() {
        verifyModel("test/data/comments/oneLineDoc.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    Test fun oneLineDocWithComment() {
        verifyModel("test/data/comments/oneLineDocWithComment.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    Test fun oneLineDocWithEmptyLine() {
        verifyModel("test/data/comments/oneLineDocWithEmptyLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("doc", content.summary.toTestString())
            }
        }
    }

    Test fun emptySection() {
        verifyModel("test/data/comments/emptySection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    Test fun explicitSummary() {
        verifyModel("test/data/comments/explicitSummary.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals("Description", content.description.toTestString())
                assertEquals(2, content.sections.count())
            }
        }
    }

    Test fun section1() {
        verifyModel("test/data/comments/section1.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one", toTestString())
                }
            }
        }
    }

    Test fun section2() {
        verifyModel("test/data/comments/section2.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(3, content.sections.count())
                with (content.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("section one", toTestString())
                }
                with (content.sections["two"]!!) {
                    assertEquals("two", label)
                    assertEquals("section two", toTestString())
                }
            }
        }
    }

    Test fun sectionOnOneLine() {
        verifyModel("test/data/comments/sectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("same line", toTestString())
                }
            }
        }
    }

    Test fun emptySectionOnOneLine() {
        verifyModel("test/data/comments/emptySectionOnOneLine.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["one"]!!) {
                    assertEquals("one", label)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    Test fun multilineSection() {
        verifyModel("test/data/comments/multilineSection.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["one"]!!) {
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
                assertEquals("Summary", content.summary.toTestString())
                assertEquals(2, content.sections.count())
                with (content.sections["this.label.is.really.long"]!!) {
                    assertEquals("this.label.is.really.long", label)
                    assertEquals("section one", toTestString())
                }
            }
        }
    }
}
