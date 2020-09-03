package renderers.gfm

import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.junit.jupiter.api.Test
import renderers.testPage
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import renderers.RawTestPage

class SimpleElementsTest : GfmRenderingOnlyTestBase() {

    @Test
    fun header() {
        val page = testPage {
            header(1, "The Hobbit or There and Back Again")
        }
        val expect = "//[testPage](test-page.md)\n\n\n\n# The Hobbit or There and Back Again  \n"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun link() {
        val page = testPage {
            link("They are not all accounted for, the lost Seeing Stones.", "http://www.google.com")
        }
        val expect = "//[testPage](test-page.md)\n\n[They are not all accounted for, the lost Seeing Stones.](http://www.google.com)"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun bold() {
        val page = testPage {
            text("That there’s some good in this world, Mr. Frodo… and it’s worth fighting for.", styles = setOf(TextStyle.Bold))
        }
        val expect = "//[testPage](test-page.md)\n\n**That there’s some good in this world, Mr. Frodo… and it’s worth fighting for.**"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun italic() {
        val page = testPage {
            text("Even the smallest person can change the course of the future.", styles = setOf(TextStyle.Italic))
        }
        val expect = "//[testPage](test-page.md)\n\n*Even the smallest person can change the course of the future.*"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun italicAndBold() {
        val page = testPage {
            text("There is no curse in Elvish, Entish, or the tongues of Men for this treachery.", styles = setOf(TextStyle.Bold, TextStyle.Italic))
        }
        val expect = "//[testPage](test-page.md)\n\n***There is no curse in Elvish, Entish, or the tongues of Men for this treachery.***"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun strikethrough() {
        val page = testPage {
            text("A day may come when the courage of men fails… but it is not THIS day", styles = setOf(TextStyle.Strikethrough))
        }
        val expect = "//[testPage](test-page.md)\n\n~~A day may come when the courage of men fails… but it is not THIS day~~"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }

    @Test
    fun images(){
        val image = ContentEmbeddedResource(
            children = emptyList(),
            address = "https://www.google.pl/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
            altText = "This is a google logo",
            dci = DCI(setOf(DRI.topLevel), ContentKind.Main),
            sourceSets = emptySet()
        )
        val page = RawTestPage(content = image)
        val expect = "//[testPage](test-page.md)\n\n![This is a google logo](https://www.google.pl/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png)"
        CommonmarkRenderer(context).render(page)
        assert(renderedContent == expect)
    }
}