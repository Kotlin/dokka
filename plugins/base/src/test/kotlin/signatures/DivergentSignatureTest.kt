package signatures

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import utils.TestOutputWriterPlugin

class DivergentSignatureTest : AbstractCoreTest() {

    val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

    val configuration = dokkaConfiguration {
        moduleName = "example"
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
            }
            val jvmAndJsSecondCommonMain = sourceSet {
                name = "jvmAndJsSecondCommonMain"
                displayName = "jvmAndJsSecondCommonMain"
                analysisPlatform = "common"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
            }
            val js = sourceSet {
                name = "js"
                displayName = "js"
                analysisPlatform = "js"
                dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
            }
            val jvm = sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
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

                assert(content.count() == 1)
                assert(content.select("[data-filterable-current=example/common example/js example/jvm]").single().brief == "common")
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
                assert(content.count() == 2)
                assert(content.select("[data-filterable-current=example/common example/jvm]").single().brief == "Time in minis common")
                assert(content.select("[data-filterable-current=example/js]").single().brief == "JS implementation of getTimeInMillis js" )
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
                assert(content.count() == 3)
                assert(content.select("[data-filterable-current=example/jvm]").single().brief == "JVM custom kdoc jvm")
                assert(content.select("[data-filterable-current=example/js]").single().brief == "JS custom kdoc js")
                assert(content.select("[data-filterable-current=example/common]").single().brief == "common")
            }
        }
    }

    private fun TestOutputWriterPlugin.renderedContent(path: String) = writer.contents.getValue(path)
            .let { Jsoup.parse(it) }.select("#content").single().select("div.divergent-group")

    private val Element.brief: String
        get() = children().select(".brief-with-platform-tags").text()
}
