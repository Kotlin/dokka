/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import renderers.testPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class CodeInlineTest : HtmlRenderingOnlyTestBase() {

    @Test
    fun `inline code without language renders with lang-kotlin class`() {
        val page = testPage {
            codeInline { text("apollo-gradle-plugin-external") }
        }
        HtmlRenderer(context).render(page)

        val el = renderedContent.selectFirst("code.lang-kotlin")
        assertNotNull(el, "Expected <code class=\"lang-kotlin\"> element")
        assertEquals("apollo-gradle-plugin-external", el.text())
        // must be inline: not wrapped in <pre>
        assertNull(el.closest("pre"), "Inline code must not be inside <pre>")
    }

    @Test
    fun `inline code with explicit language renders with that language class`() {
        val page = testPage {
            codeInline(language = "custom") { text("myCode") }
        }
        HtmlRenderer(context).render(page)

        val el = renderedContent.selectFirst("code.lang-custom")
        assertNotNull(el, "Expected <code class=\"lang-custom\"> element")
        assertEquals("myCode", el.text())
        assertNull(el.closest("pre"), "Inline code must not be inside <pre>")
    }

    @Test
    fun `inline code without language does not produce a code block`() {
        val page = testPage {
            codeInline { text("someIdentifier") }
        }
        HtmlRenderer(context).render(page)

        // block code is wrapped in <pre>; inline code must not be
        assertEquals(0, renderedContent.select("pre").size)
    }
}
