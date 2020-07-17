package renderers.gfm

import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.junit.jupiter.api.Test
import renderers.TestPage
import org.jetbrains.dokka.base.translators.documentables.*
import org.jetbrains.dokka.pages.TextStyle

class SimpleElementsTest : GfmRenderingOnlyTestBase() {

    @Test
    fun header() {
        val page = TestPage {
            header(1, "The Hobbit or There and Back Again")
        }
        val expect = "//[testPage](test-page.md)\n\n\n\n# The Hobbit or There and Back Again  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun link() {
        val page = TestPage {
            link("They are not all accounted for, the lost Seeing Stones.", "http://www.google.com")
        }
        val expect = "//[testPage](test-page.md)\n\n[They are not all accounted for, the lost Seeing Stones.](http://www.google.com)"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun bold() {
        val page = TestPage {
            text("That there’s some good in this world, Mr. Frodo… and it’s worth fighting for.", styles = setOf(TextStyle.Bold))
        }
        val expect = "//[testPage](test-page.md)\n\n**That there’s some good in this world, Mr. Frodo… and it’s worth fighting for.**"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun italic() {
        val page = TestPage {
            text("Even the smallest person can change the course of the future.", styles = setOf(TextStyle.Italic))
        }
        val expect = "//[testPage](test-page.md)\n\n*Even the smallest person can change the course of the future.*"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun italicAndBold() {
        val page = TestPage {
            text("There is no curse in Elvish, Entish, or the tongues of Men for this treachery.", styles = setOf(TextStyle.Bold, TextStyle.Italic))
        }
        val expect = "//[testPage](test-page.md)\n\n***There is no curse in Elvish, Entish, or the tongues of Men for this treachery.***"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun strikethrough() {
        val page = TestPage {
            text("A day may come when the courage of men fails… but it is not THIS day", styles = setOf(TextStyle.Strikethrough))
        }
        val expect = "//[testPage](test-page.md)\n\n~~A day may come when the courage of men fails… but it is not THIS day~~"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}