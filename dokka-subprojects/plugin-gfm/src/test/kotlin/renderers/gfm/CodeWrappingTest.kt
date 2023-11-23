/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.gfm

import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import renderers.testPage
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeWrappingTest : GfmRenderingOnlyTestBase() {
    @Test
    fun wrappedCodeBlock() {
        val page = testPage {
            codeBlock {
                text("fun myCode(): String")
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |```kotlin
                        |fun myCode(): String
                        |```""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assertEquals(expect, renderedContent)
    }

    @Test
    fun `should preserve original text without escaping`() {
        val page = testPage {
            codeBlock {
                text("<----> **text** & ~~this~~  and \"that\"")
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |```kotlin
                        |<----> **text** & ~~this~~  and "that"
                        |```""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assertEquals(expect, renderedContent)
    }


    @Test
    fun wrappedInlineCode() {
        val page = testPage {
            text("This function adds the values of ")
            codeInline {
                text("left") 
            }
            text(" and ")
            codeInline {
                text("right")
            }
            text(".\nBoth numbers must be finite, or an exception occurs.\n")
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |This function adds the values of `left` and `right`.
                        |Both numbers must be finite, or an exception occurs.""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assertEquals(expect, renderedContent)
    }

    @Test
    fun `should not add trailing backslash to newline elements for code inline code`() {
        val page = testPage {
            text("This adds some symbols (")
            codeInline {
                text("<----> **text** & ~~this~~  and \"that\"")
            }
            text(") to the test")
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |This adds some symbols (`<----> **text** & ~~this~~  and "that"`) to the test""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assertEquals(expect, renderedContent)
    }
}
