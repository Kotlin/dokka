package signatures

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import java.nio.file.Paths

class DivergentSignatureTest : AbstractCoreTest() {

    private val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                moduleName = "example"
                displayName = "common"
                name = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("commonMain").map {
                    Paths.get("$testDataDir/$it/kotlin").toString()
                }
            }

            sourceSet {
                moduleName = "example"
                displayName = "js"
                name = "js"
                analysisPlatform = "js"
                dependentSourceSets = setOf(DokkaSourceSetID("example", "common"))
                sourceRoots = listOf("jsMain").map {
                    Paths.get("$testDataDir/$it/kotlin").toString()
                }
            }

            sourceSet {
                moduleName = "example"
                displayName = "jvm"
                name = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(DokkaSourceSetID("example", "common"))
                sourceRoots = listOf("jvmMain").map {
                    Paths.get("$testDataDir/$it/kotlin").toString()
                }
            }
        }
    }

    @Test
    fun `group { common + jvm + js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedContent("example/example/-clock/get-time.html")

                assertEquals(1, content.size)
                assertEquals(
                    "", content.select("[data-filterable-current=example/js example/jvm example/common]").single().brief
                )
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
                val content = writerPlugin.renderedContent("example/example/-clock/get-times-in-millis.html")
                assertEquals(2, content.size)
                assertEquals(
                    "Time in minis",
                    content.select("[data-filterable-current=example/jvm example/common]").single().brief
                )
                assertEquals(
                    "JS implementation of getTimeInMillis js",
                    content.select("[data-filterable-current=example/js]").single().brief
                )
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
                val content = writerPlugin.renderedContent("example/example/-clock/get-year.html")
                assertEquals(3, content.size)
                assertEquals(
                    "JVM custom kdoc jvm",
                    content.select("[data-filterable-current=example/jvm]").single().brief
                )
                assertEquals("JS custom kdoc js", content.select("[data-filterable-current=example/js]").single().brief)
                assertEquals("common", content.select("[data-filterable-current=example/common]").single().brief)
            }
        }
    }

    private fun TestOutputWriterPlugin.renderedContent(path: String) = writer.contents.getValue(path)
        .let { Jsoup.parse(it) }.select("#content").single().select("div.divergent-group")

    private val Element.brief: String
        get() = children().select(".brief-with-platform-tags").text()
}
