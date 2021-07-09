package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import renderers.testPage
import utils.B
import utils.I
import utils.STRIKE
import utils.match

class TextStylesTest : HtmlRenderingOnlyTestBase() {
    @Test
    fun `should include bold`(){
        val page = testPage {
            text("bold text", styles = setOf(TextStyle.Bold))
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(B("bold text"))
    }

    @Test
    fun `should include italics`(){
        val page = testPage {
            text("italics text", styles = setOf(TextStyle.Italic))
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(I("italics text"))
    }

    @Test
    fun `should include strikethrought`(){
        val page = testPage {
            text("strike text", styles = setOf(TextStyle.Strikethrough))
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(STRIKE("strike text"))
    }

    @Test
    fun `should include multiple styles at one`(){
        val page = testPage {
            text(
                "styled text",
                styles = setOf(
                    TextStyle.Strikethrough,
                    TextStyle.Bold,
                    TextStyle.Indented,
                    TextStyle.UnderCoverText,
                    TextStyle.BreakableAfter
                )
            )
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(STRIKE(B("styled text")))
        //Our dsl swallows nbsp so i manually check for it
        files.contents.getValue("test-page.html").contains("&nbsp;<strike><b>styled text</b></strike>")
    }


    override val renderedContent: Element
        get() = files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select("#content").single()
}