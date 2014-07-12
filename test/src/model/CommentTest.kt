package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class CommentTest {
    Test fun emptyDoc() {
        verifyModel("test/data/comments/emptyDoc.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationContent.Empty, doc)
            }
        }
    }

    Test fun emptyDocButComment() {
        verifyModel("test/data/comments/emptyDocButComment.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationContent.Empty, doc)
            }
        }
    }

    Test fun multilineDoc() {
        verifyModel("test/data/comments/multilineDoc.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("""doc1
doc2
doc3""", doc.summary)
            }
        }
    }

    Test fun multipleDocs() {
        verifyModel("test/data/comments/multipleDocs.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("""doc1
doc2
doc3""", doc.summary)
            }
        }
    }

    Test fun multilineDocWithComment() {
        verifyModel("test/data/comments/multilineDocWithComment.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("""doc1
doc2
doc3""", doc.summary)
            }
        }
    }

    Test fun oneLineDoc() {
        verifyModel("test/data/comments/oneLineDoc.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("doc", doc.summary)
            }
        }
    }

    Test fun oneLineDocWithComment() {
        verifyModel("test/data/comments/oneLineDocWithComment.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("doc", doc.summary)
            }
        }
    }

    Test fun oneLineDocWithEmptyLine() {
        verifyModel("test/data/comments/oneLineDocWithEmptyLine.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("doc", doc.summary)
            }
        }
    }

    Test fun emptySection() {
        verifyModel("test/data/comments/emptySection.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("", text)
                }
            }
        }
    }

    Test fun section1() {
        verifyModel("test/data/comments/section1.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("section one", text)
                }
            }
        }
    }

    Test fun section2() {
        verifyModel("test/data/comments/section2.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(2, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("section one", text)
                }
                with (doc.sections.elementAt(1)) {
                    assertEquals("two", label)
                    assertEquals("section two", text)
                }
            }
        }
    }

    Test fun sectionOnOneLine() {
        verifyModel("test/data/comments/sectionOnOneLine.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("same line", text)
                }
            }
        }
    }

    Test fun emptySectionOnOneLine() {
        verifyModel("test/data/comments/emptySectionOnOneLine.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("", text)
                }
            }
        }
    }

    Test fun multilineSection() {
        verifyModel("test/data/comments/multilineSection.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("one", label)
                    assertEquals("""line one
line two""", text)
                }
            }
        }
    }

    Test fun sectionWithBracedLabel() {
        verifyModel("test/data/comments/sectionWithBracedLabel.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("Summary", doc.summary)
                assertEquals(1, doc.sections.count())
                with (doc.sections.elementAt(0)) {
                    assertEquals("this.label.is.really.long", label)
                    assertEquals("section one", text)
                }
            }
        }
    }
}
