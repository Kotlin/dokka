/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.pages.TokenStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import renderers.testPage
import utils.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `should include token styles`(){
        val page = testPage {
            text("keyword", styles = setOf(TokenStyle.Keyword))
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(Span("keyword"))
        val lastChild = renderedContent.children().last() ?: throw IllegalStateException("No element found")
        assertEquals(lastChild.attr("class"), "token keyword")
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

    @Test
    fun `should include blockquote`() {
        val page = testPage {
            group(styles = setOf(TextStyle.Quotation)) {
                text("blockquote text")
            }
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(BlockQuote("blockquote text"))
    }

    @Test
    fun `should include var`() {
        val page = testPage {
            group(styles = setOf(TextStyle.Var)) {
                text("variable")
            }
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(Var("variable"))
    }

    @Test
    fun `should include underlined text`() {
        val page = testPage {
            group(styles = setOf(TextStyle.Underlined)) {
                text("underlined text")
            }
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(U("underlined text"))
    }

    override val renderedContent: Element
        get() = files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select("#content").single()
}
