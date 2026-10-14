/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Suppress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KDocSectionSplittingTest {

    private fun parse(text: String, externalDri: (String) -> org.jetbrains.dokka.links.DRI? = { null }): DocumentationNode =
        MarkdownParser(externalDri, "test").parse(text)

    private fun plainText(tag: DocTag): String = buildString {
        fun visit(t: DocTag) {
            if (t is Text) append(t.body)
            t.children.forEach { visit(it) }
        }
        visit(tag)
    }

    @Test
    fun `multiple tags in one comment`() {
        val doc = parse(
            """
            |Leading description.
            |
            |@param foo the foo param
            |@return the result
            """.trimMargin()
        )

        assertEquals(3, doc.children.size)
        (doc.children[0] as Description)
        assertTrue(plainText(doc.children[0].root).contains("Leading description."))

        val param = (doc.children[1] as Param)
        assertEquals("foo", param.name)
        assertTrue(plainText(param.root).contains("the foo param"))

        val ret = (doc.children[2] as Return)
        assertTrue(plainText(ret.root).contains("the result"))
    }

    @Test
    fun `at inside fenced code block is not a new tag`() {
        val doc = parse(
            """
            |Example:
            |```
            |@Annotation
            |fun foo() {}
            |```
            |@return done
            """.trimMargin()
        )

        assertEquals(2, doc.children.size)
        (doc.children[0] as Description)
        assertTrue(plainText(doc.children[0].root).contains("@Annotation"))
        val ret = (doc.children[1] as Return)
        assertTrue(plainText(ret.root).contains("done"))
    }

    @Test
    fun `escaped at is not a new tag`() {
        val doc = parse(
            """
            |See \@Override for details.
            |@return done
            """.trimMargin()
        )

        assertEquals(2, doc.children.size)
        assertTrue(plainText(doc.children[0].root).contains("@Override"))
        (doc.children[1] as Return)
    }

    @Test
    fun `unknown custom tag`() {
        val doc = parse("@customTag some body text")

        assertEquals(2, doc.children.size)
        val custom = (doc.children[1] as CustomTagWrapper)
        assertEquals("customTag", custom.name)
        assertTrue(plainText(custom.root).contains("some body text"))
    }

    @Test
    fun `tag with no body does not crash`() {
        val doc = parse(
            """
            |Desc.
            |@suppress
            """.trimMargin()
        )

        assertEquals(2, doc.children.size)
        (doc.children[1] as Suppress)
    }

    @Test
    fun `two tags with no blank line between them`() {
        val doc = parse(
            """
            |@param a first
            |@param b second
            """.trimMargin()
        )

        assertEquals(3, doc.children.size)
        val first = (doc.children[1] as Param)
        assertEquals("a", first.name)
        assertTrue(plainText(first.root).contains("first"))
        val second = (doc.children[2] as Param)
        assertEquals("b", second.name)
        assertTrue(plainText(second.root).contains("second"))
    }

    @Test
    fun `at-looking text inside a blockquote is not a new tag`() {
        val doc = parse(
            """
            |> @notATag inside quote
            |
            |@return done
            """.trimMargin()
        )

        assertEquals(2, doc.children.size)
        (doc.children[0] as Description)
        assertTrue(plainText(doc.children[0].root).contains("@notATag"))
        (doc.children[1] as Return)
    }

    @Test
    fun `link definition in one tag resolves for a reference in another tag`() {
        val doc = parse(
            """
            |@param a see [link]
            |@return see [link] too
            |
            |[link]: https://example.com
            """.trimMargin()
        )

        assertEquals(3, doc.children.size)
        val param = (doc.children[1] as Param)
        assertTrue(plainText(param.root).contains("link"))
        val ret = (doc.children[2] as Return)
        assertTrue(plainText(ret.root).contains("link"))
    }

    @Test
    fun `see and throws resolve subject via externalDri`() {
        val resolved = mutableListOf<String>()
        val doc = parse(
            """
            |@see Foo
            |@throws Bar oops
            """.trimMargin()
        ) { name -> resolved += name; null }

        assertEquals(listOf("Foo", "Bar"), resolved)
        val see = (doc.children[1] as See)
        assertEquals("Foo", see.name)
        val throws = (doc.children[2] as Throws)
        assertEquals("Bar", throws.name)
    }
}
