package renderers.gfm

import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import renderers.*

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
    fun wrappedInlineCode() {
        val page = testPage {
            text("This function adds the values of ")
            codeInline("") { 
                text("left") 
            }
            text(" and ")
            codeInline("") {
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
}
