/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.TextStyle
import renderers.testPage
import utils.Div
import utils.P
import utils.match
import kotlin.test.Test

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
