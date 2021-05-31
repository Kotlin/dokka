package signatures

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals

class DivergentSignatureTest : AbstractRenderingTest() {

    @Test
    fun `group { common + jvm + js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedDivergentContent("example/example/-clock/get-time.html")

                assert(content.count() == 1)
                assert(content.select("[data-filterable-current=example/common example/js example/jvm]").single().brief == "")
            }
        }
    }

    @Test
    fun `group { common + jvm }, group { js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedDivergentContent("example/example/-clock/get-times-in-millis.html")
                assert(content.count() == 2)
                assert(content.select("[data-filterable-current=example/common example/jvm]").single().brief == "Time in minis")
                assert(content.select("[data-filterable-current=example/js]").single().brief == "JS implementation of getTimeInMillis" )
            }
        }
    }

    @Test
    fun `group { js }, group { jvm }, group { js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedDivergentContent("example/example/-clock/get-year.html")
                assert(content.count() == 3)
                assert(content.select("[data-filterable-current=example/jvm]").single().brief == "JVM custom kdoc")
                assert(content.select("[data-filterable-current=example/js]").single().brief == "JS custom kdoc")
                assert(content.select("[data-filterable-current=example/common]").single().brief == "")
            }
        }
    }
}
