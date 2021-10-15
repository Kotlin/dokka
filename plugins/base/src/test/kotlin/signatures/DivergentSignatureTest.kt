package signatures

import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class DivergentSignatureTest : AbstractRenderingTest() {

    @Test
    fun `group { common + jvm + js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDepenentContent("example/example/-clock/get-time.html")

                assert(content.count() == 3)
                val sourceSets = listOf("example/common", "example/js", "example/jvm")
                sourceSets.forEach {
                    assert(content.select("[data-togglable=$it]").single().brief == "")
                }
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
                val content = writerPlugin.renderedSourceDepenentContent("example/example/-clock/get-times-in-millis.html")

                assert(content.count() == 3)
                assert(content.select("[data-togglable=example/common]").single().brief == "Time in minis")
                assert(content.select("[data-togglable=example/jvm]").single().brief == "Time in minis")
                assert(content.select("[data-togglable=example/js]").single().brief == "JS implementation of getTimeInMillis" )
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
                val content = writerPlugin.renderedSourceDepenentContent("example/example/-clock/get-year.html")
                assert(content.count() == 3)
                assert(content.select("[data-togglable=example/jvm]").single().brief == "JVM custom kdoc")
                assert(content.select("[data-togglable=example/js]").single().brief == "JS custom kdoc")
                assert(content.select("[data-togglable=example/common]").single().brief == "")
            }
        }
    }
}
