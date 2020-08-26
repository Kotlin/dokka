package signatures

import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.A
import utils.Span
import utils.TestOutputWriterPlugin
import utils.match

class VarianceSignatureTest : AbstractCoreTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    fun source(signature: String) =
        """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | $signature
            """.trimIndent()

    @Test
    fun `simple contravariance`() {
        val source = source("class Generic<in T>")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", A("Generic"), "<in ", A("T"), ">", Span()
                )
            }
        }
    }

    @Test
    fun `simple covariance`() {
        val source = source("class Generic<out T>")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", A("Generic"), "<out ", A("T"), ">", Span()
                )
            }
        }
    }

    @Test
    fun `simple invariance`() {
        val source = source("class Generic<T>")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", A("Generic"), "<", A("T"), ">", Span()
                )
            }
        }
    }

    @Test
    fun `covariance and bound`() {
        val source = source("class Generic<out T : List<CharSequence>>")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", A("Generic"), "<out ", A("T"), ":", A("List"), "<", A("CharSequence"), ">>", Span()
                )
            }
        }
    }
}

