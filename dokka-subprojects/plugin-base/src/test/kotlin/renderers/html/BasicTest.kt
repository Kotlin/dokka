/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.links.DRI
import renderers.testPage
import utils.Span
import utils.match
import kotlin.test.Test

class BasicTest : HtmlRenderingOnlyTestBase() {
    @Test
    fun `unresolved DRI link should render as text`() {
        val page = testPage {
            link("linkText", DRI("nonexistentPackage", "nonexistentClass"))
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Span("linkText"))
    }
}
