package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import renderers.*

class GroupWrappingTest: RenderingOnlyTestBase() {

    @Test
    fun notWrapped() {
        val page = TestPage {
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
        val page = TestPage {
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
        val page = TestPage {
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

        HtmlRenderer(context).render(page)

        renderedContent.match(Div("a", Div(Div("bc")), "d"))
    }

}
