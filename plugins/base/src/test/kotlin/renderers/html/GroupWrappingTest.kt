package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.junit.Test
import renderers.RenderingOnlyTestBase
import renderers.TestPage

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

        assert(linesAfterContentTag().contains("abc"))
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

        assert(linesAfterContentTag().contains("<p>ab</p>c"))
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

        assert(linesAfterContentTag().contains("<div>ab</div>c"))
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

        assert(linesAfterContentTag().contains("<div>a<div><div>bc</div></div>d</div>"))
    }

}
