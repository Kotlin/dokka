package mathjaxTest

import org.jetbrains.dokka.mathjax.MathjaxPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class MathjaxPluginTest : AbstractCoreTest() {
    @Test
    fun basicMathjaxTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * {@usesMathJax}
            | *
            | * <p>\(\alpha_{out} = \alpha_{dst}\)</p>
            | * <p>\(C_{out} = C_{dst}\)</p>
            | */
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin())
        ) {
            renderingStage = {
                    _, _ -> Jsoup
                .parse(writerPlugin.writer.contents["root/example.html"])
                .head()
                .select("link, script")
                .let {
                    val link = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"
                    assert(it.`is`("[href=$link], [src=$link]"))
                }
            }
        }
    }

    @Test
    fun basicNoMathjaxTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * <p>\(\alpha_{out} = \alpha_{dst}\)</p>
            | * <p>\(C_{out} = C_{dst}\)</p>
            | */
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin())
        ) {
            renderingStage = {
                    _, _ -> Jsoup
                .parse(writerPlugin.writer.contents["root/example.html"])
                .head()
                .select("link, script")
                .let {
                    val link = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"
                    assert(!it.`is`("[href=$link], [src=$link]"))
                }
            }
        }
    }
}
