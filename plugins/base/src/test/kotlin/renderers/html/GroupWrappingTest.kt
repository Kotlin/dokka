package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.*
import utils.Div
import utils.P
import utils.match

class GroupWrappingTest : HtmlRenderingOnlyTestBase() {

    @Test
    fun notWrapped() {
        val page = testPage {
            group {
                text("a")
                text("b")
            }
            text("c")
        }

        HtmlRenderer(context).render(page)

        renderedContent.match("abc")
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

        HtmlRenderer(context).render(page)

        renderedContent.match(P("ab"), "c")
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

        HtmlRenderer(context).render(page)

        renderedContent.match(Div("ab"), "c")
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

        HtmlRenderer(context).render(page)

        renderedContent.match(Div("a", Div(Div("bc")), "d"))
    }

}
