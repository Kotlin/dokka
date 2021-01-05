package renderers.html

import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Test
import renderers.testPage
import utils.Span
import utils.match

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
