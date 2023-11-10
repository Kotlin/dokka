/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.pages.ListStyle
import renderers.testPage
import utils.Dd
import utils.Dl
import utils.Dt
import utils.match
import kotlin.test.Test


class ListStylesTest : HtmlRenderingOnlyTestBase() {

    @Test
    fun `description list render`() {
        val page = testPage {
            descriptionList {
                item(styles = setOf(ListStyle.DescriptionTerm)) {
                    text("Description term #1")
                }
                item(styles = setOf(ListStyle.DescriptionTerm)) {
                    text("Description term #2")
                }
                item(styles = setOf(ListStyle.DescriptionDetails)) {
                    text("Description details describing terms #1 and #2")
                }
            }
        }


        HtmlRenderer(context).render(page)
        renderedContent.match(
            Dl(
                Dt("Description term #1"),
                Dt("Description term #2"),
                Dd("Description details describing terms #1 and #2")
            )
        )
    }
}
