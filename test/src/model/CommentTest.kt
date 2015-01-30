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
                assertEquals(1, content.sections.count())
                with (content.findSectionByTag("one")!!) {
                    assertEquals("One", tag)
                    assertEquals("", toTestString())
                }
            }
        }
    }

    Test fun section1() {
        verifyModel("test/data/comments/section1.kt") { model ->
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

    Test fun section2() {
        verifyModel("test/data/comments/section2.kt") { model ->
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

    Test fun multilineSection() {
        verifyModel("test/data/comments/multilineSection.kt") { model ->
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

    Test fun directive() {
        verifyModel("test/data/comments/directive.kt") { model ->
            with(model.members.single().members.first()) {
                assertEquals("Summary", content.summary.toTestString())
                with (content.description) {
                    assertEqualsIgnoringSeparators("""[code]
if (true) {
    println(property)
}
[/code]
[code]
if (true) {
    println(property)
}
[/code]
[code]
if (true) {
    println(property)
}
[/code]
[code]
if (true) {
    println(property)
}
[/code]
""", toTestString())
                }
            }
        }
    }
}
