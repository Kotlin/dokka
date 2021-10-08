package renderers.gfm

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.gfm.GfmPlugin
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class ParseRenderTest : BaseAbstractTest() {

    @Test
    fun `deprecated fun markdown`() {
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
            | * Just a deprecated function
            | */
            | @Deprecated("This is deprecated")
            | fun simpleFun(test: Int): String = "This is the one ring"
            """.trimIndent()
        val expected =
            """
            //[root](../../index.md)/[example](index.md)
            
            # Package example

            ## Functions

            | Name | Summary |
            |---|---|
            | [simpleFun](simple-fun.md) | [JVM]<br><s>fun</s> [<s>simpleFun</s>](simple-fun.md)<s>(</s><s>test</s><s>:</s> Int<s>)</s><s>:</s> String<br>Just a deprecated function |
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, GfmPlugin())
        ) {
            renderingStage = { _, _ ->
                assertEquals(expected, writerPlugin.writer.contents["root/example/index.md"])
            }
        }
    }
}