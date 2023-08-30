/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.A
import utils.TestOutputWriterPlugin
import utils.match
import kotlin.test.Test

class VarianceSignatureTest : BaseAbstractTest() {
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
                    "class ", A("Generic"), "<in ", A("T"), ">",
                    ignoreSpanWithTokenStyle = true
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
                    "class ", A("Generic"), "<out ", A("T"), ">",
                    ignoreSpanWithTokenStyle = true
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
                    "class ", A("Generic"), "<", A("T"), ">",
                    ignoreSpanWithTokenStyle = true
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
                    "class ", A("Generic"), "<out ", A("T"), ":", A("List"), "<", A("CharSequence"), ">>",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}

