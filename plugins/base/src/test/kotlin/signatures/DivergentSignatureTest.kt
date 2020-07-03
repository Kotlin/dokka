package signatures

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.*
import java.nio.file.Paths

class DivergentSignatureTest : AbstractCoreTest() {

    @Test
    fun `three divergent signatures for class`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()


        val configuration = dokkaConfiguration {
            passes {
                pass {
                    moduleName = "example"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "js"
                }
                pass {
                    moduleName = "example"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "jvm"
                }
                pass {
                    moduleName = "example"
                    analysisPlatform = "common"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "jvm"
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                assert(writerPlugin.writer.contents.getValue("example/example/-clock/index.html")
                    .let { Jsoup.parse(it) }.select("#content").single()
                    .select("div.sourceset-depenent-content")
                    .fold(0) { acc, elem ->
                        acc + if (elem.child(0).html().contains(
                                Regex("Documentation for (expected|actual) class Clock .*")
                            )
                        ) 1 else 0
                    } == 3
                )
            }
        }
    }

    @Test
    fun `three divergent signatures for class`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()


        val configuration = dokkaConfiguration {
            passes {
                pass {
                    moduleName = "example"
                    analysisPlatform = "js"
                    sourceRoots = listOf("jsMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "js"
                }
                pass {
                    moduleName = "example"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "jvm"
                }
                pass {
                    moduleName = "example"
                    analysisPlatform = "common"
                    sourceRoots = listOf("jvmMain", "commonMain", "jvmAndJsSecondCommonMain").map {
                        Paths.get("$testDataDir/$it/kotlin").toString()
                    }
                    sourceSetID = "jvm"
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                assert(writerPlugin.writer.contents.getValue("example/example/-clock/index.html")
                    .let { Jsoup.parse(it) }.select("#content").single()
                    .select("div.sourceset-depenent-content")
                    .fold(0) { acc, elem ->
                        acc + if (elem.child(0).html().contains(
                                Regex("Documentation for (expected|actual) class Clock .*")
                            )
                        ) 1 else 0
                    } == 3
                )
            }
        }
    }
}