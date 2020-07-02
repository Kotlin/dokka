package renderers.gfm

import org.jetbrains.dokka.gfm.CommonmarkRenderer
import renderers.gfm.GfmRenderingOnlyTestBase
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.*

class GroupWrappingTest : GfmRenderingOnlyTestBase() {

    @Test
    fun notWrapped() {
        val page = TestPage {
            group {
                text("a")
                text("b")
            }
            text("c")
        }

        CommonmarkRenderer(context).render(page)

        assert(renderedContent == "//[testPage](test-page.md)\n\nabc")
    }

    @Test
    fun paragraphWrapped() {
        val page = TestPage {
            group(styles = setOf(TextStyle.Paragraph)) {
                text("a")
                text("b")
            }
            text("c")
        }

        CommonmarkRenderer(context).render(page)

        assert(renderedContent == "//[testPage](test-page.md)\n\n\n\nab\n\nc")
    }

    @Test
    fun blockWrapped() {
        val page = TestPage {
            group(styles = setOf(TextStyle.Block)) {
                text("a")
                text("b")
            }
            text("c")
        }

        CommonmarkRenderer(context).render(page)

        assert(renderedContent == "//[testPage](test-page.md)\n\nab  \nc")
    }

    @Test
    fun nested() {
        val page = TestPage {
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

        CommonmarkRenderer(context).render(page)

//        renderedContent.match(Div("a", Div(Div("bc")), "d"))
        assert(renderedContent == "//[testPage](test-page.md)\n\nabc  \n  \nd  \n")
    }

}
