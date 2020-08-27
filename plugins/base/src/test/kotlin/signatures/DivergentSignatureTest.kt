package signatures

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import utils.TestOutputWriterPlugin

class DivergentSignatureTest : AbstractCoreTest() {

    @Test
    fun `group { common + jvm + js }`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                sourceSet {
                    displayName = "js"
                    name = "js"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "jvm"
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "common"
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedContent("example/example/-clock/get-time.html")

                assert(content.count() == 1)
                assert(content.select("[data-filterable-current=example/js example/jvm example/common]").single().brief == "")
            }
        }
    }

    @Test
    fun `group { common + jvm }, group { js }`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                sourceSet {
                    displayName = "js"
                    name = "js"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "jvm"
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "common"
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedContent("example/example/-clock/get-times-in-millis.html")
                assert(content.count() == 2)
                assert(content.select("[data-filterable-current=example/jvm example/common]").single().brief == "Time in minis")
                assert(content.select("[data-filterable-current=example/js]").single().brief == "JS implementation of getTimeInMillis js" )
            }
        }
    }

    @Test
    fun `group { js }, group { jvm }, group { js }`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                sourceSet {
                    displayName = "js"
                    name = "js"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "jvm"
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
                sourceSet {
                    displayName = "common"
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("commonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                }
            }
        }

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
