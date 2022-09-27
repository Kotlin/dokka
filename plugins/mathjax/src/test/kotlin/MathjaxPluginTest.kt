package mathjaxTest

import org.jetbrains.dokka.mathjax.LIB_PATH
import org.jetbrains.dokka.mathjax.MathjaxPlugin
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class MathjaxPluginTest : BaseAbstractTest() {
    @Test
    fun noMathjaxTest() {
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
            | * Just a regular kdoc
            | */
            | fun test(): String = ""
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin())
        ) {
            renderingStage = {
                    _, _ -> Jsoup
                .parse(writerPlugin.writer.contents.getValue("root/example/test.html"))
                .head()
                .select("link, script")
                .let {
                    assert(!it.`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))
                }
            }
        }
    }

    @Test
    fun usingMathjaxTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val math = "a^2 = b^2 + c^2"
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * @usesMathJax
            | * 
            | * \($math\)
            | */
            | fun test(): String = ""
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin())
        ) {
            renderingStage = { _, _ ->
                val parsed = Jsoup.parse(writerPlugin.writer.contents.getValue("root/example/test.html"))

                // Ensure the MathJax CDN is loaded
                assert(parsed.select("link, script").`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))

                // Ensure the contents are displayed
                assert(parsed.select("p").any {
                    it.text().contains(math)
                })
            }
        }
    }
}
