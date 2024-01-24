/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package mathjaxTest

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.mathjax.LIB_PATH
import org.jetbrains.dokka.mathjax.MathjaxPlugin
import org.jsoup.Jsoup
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertTrue

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
                    assertTrue(!it.`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))
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
                assertTrue(parsed.select("link, script").`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))

                // Ensure the contents are displayed
                assertTrue(parsed.select("p").any {
                    it.text().contains(math)
                })
            }
        }
    }
}
