package renderers.gfm

import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.*

class GroupWrappingTest : GfmRenderingOnlyTestBase() {

    @Test
    fun notWrapped() {
        val page = testPage {
            group {
                text("a")
                text("b")
            }
            text("c")
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |abc""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun paragraphWrapped() {
        val page = testPage {
            group(styles = setOf(TextStyle.Paragraph)) {
                text("a")
                text("b")
            }
            text("c")
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |ab
                        |
                        |c""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun blockWrapped() {
        val page = testPage {
            group(styles = setOf(TextStyle.Block)) {
                text("a")
                text("b")
            }
            text("c")
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |ab
                        |
                        |c""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun nested() {
        val page = testPage {
            group(styles = setOf(TextStyle.Block)) {
                text("a")
                group(styles = setOf(TextStyle.Block)) {
                    group(styles = setOf(TextStyle.Block)) {
                        text("b")
                        text("c")
                    }
                }
                text("d")
            }
        }
        val expect = """|//[testPage](test-page.md)
                        |
                        |a
                        |
                        |bc
                        |
                        |d""".trimMargin()

        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}
