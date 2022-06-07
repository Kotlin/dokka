package kotlinAsJavaPlugin

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.jdk
import org.junit.jupiter.api.Test
import signatures.firstSignature
import signatures.renderedContent
import utils.*

class KotlinAsJavaSignatureTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                externalDocumentationLinks = listOf(
                    DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                    stdlibExternalDocumentationLink
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun source(signature: String) =
        """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            | $signature
            """.trimIndent()

    @Test
    fun `fun with definitely non-nullable types as java`() {
        val source = source("fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signature = writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-test-kt/elvis-like.html").firstSignature()
                signature.match(
                    "public final static ", Span("T"), A("elvisLike"),
                    "<T extends ", A("Any"), ">(",
                    Span(
                        Span(Span(), " x, "),
                        Span(Span(), " y")
                    ),
                    ")", Span(),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}