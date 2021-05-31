package signatures

import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawHtmlRenderingTest: AbstractRenderingTest() {
    @Test
    fun `work with raw html with inline comment`() {
        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedDivergentContent("example/example/-html-test/test.html")
                assert(content.count() == 1)
                assertEquals(content.select("[data-filterable-current=example/jvm]").single().rawBrief,"This is an example <!-- not visible --> of html")

                val indexContent = writerPlugin.writer.contents.getValue("example/example/-html-test/index.html")
                    .let { Jsoup.parse(it) }
                assertTrue(indexContent.select("div.brief").any { it.html().contains("This is an example <!-- not visible --> of html")})
            }
        }
    }

    @Test
    fun `work with raw html`() {
        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                //Module page
                val content = writerPlugin.renderedContent("example/example/index.html").select("div.brief")
                assertTrue(content.size > 0)
                assertTrue(content.any { it.html().contains("<!-- this shouldn't be visible -->")})
            }
        }
    }

    @Test
    fun `work with raw, visible html`() {
        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedDivergentContent("example/example/-html-test/test-p.html")
                assert(content.count() == 1)
                assertEquals(content.select("[data-filterable-current=example/jvm]").single().rawBrief, "This is an <b> documentation </b>")

                val indexContent = writerPlugin.writer.contents.getValue("example/example/-html-test/index.html")
                    .let { Jsoup.parse(it) }
                assertTrue(indexContent.select("div.brief").any { it.html().contains("This is an <b> documentation </b>")})
            }
        }
    }
}