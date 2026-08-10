/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import signatures.firstSignature
import signatures.renderedContent
import utils.A
import utils.TestOutputWriterPlugin
import utils.match
import kotlin.test.Test
import kotlin.text.trimIndent

class VarianceSignatureTest : org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest() {
    private val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                sourceRoots = kotlin.collections.listOf("src/")
                classpath =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath!!)
                externalDocumentationLinks =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
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

    @kotlin.test.Test
    fun `simple contravariance`() {
        val source = source("class Generic<in T>")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", utils.A("Generic"), "<in ", utils.A("T"), ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `simple covariance`() {
        val source = source("class Generic<out T>")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", utils.A("Generic"), "<out ", utils.A("T"), ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `simple invariance`() {
        val source = source("class Generic<T>")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ", utils.A("Generic"), "<", utils.A("T"), ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `covariance and bound`() {
        val source = source("class Generic<out T : List<CharSequence>>")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic/index.html").firstSignature().match(
                    "class ",
                    utils.A("Generic"),
                    "<out ",
                    utils.A("T"),
                    ":",
                    utils.A("List"),
                    "<",
                    utils.A("CharSequence"),
                    ">>",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}

