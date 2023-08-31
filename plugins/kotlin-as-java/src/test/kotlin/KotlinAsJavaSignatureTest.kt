/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinAsJavaPlugin

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.jdk
import signatures.firstSignature
import signatures.renderedContent
import signatures.signature
import utils.*
import kotlin.test.Test

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
                    ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `should display annotations`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |@MustBeDocumented
            |annotation class OnClass
            |
            |@MustBeDocumented
            |annotation class OnMethod
            |
            |@MustBeDocumented
            |annotation class OnParameter
            |
            |@Target(AnnotationTarget.TYPE)
            |@MustBeDocumented
            |annotation class OnType
            |
            |@Target(AnnotationTarget.TYPE_PARAMETER)
            |@MustBeDocumented
            |annotation class OnTypeParameter
            |
            |@OnClass
            |class Clazz<@OnTypeParameter T : @OnType Any> {
            |    @OnMethod
            |    fun <@OnTypeParameter T : @OnType Any> withParams(@OnParameter str1: String, str2: String): Boolean {
            |        return str1 == str2
            |    }
            |}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer
                    .renderedContent("root/kotlinAsJavaPlugin/-clazz/index.html")
                    .signature()

                val classSignature = signatures[0]
                classSignature.match(
                    Div(Div("@", A("OnClass"), "()")),
                    "public final class ", A("Clazz"),
                    // <@OnTypeParameter() T extends @OnType() Object>
                    "<", Span("@", A("OnTypeParameter"), "() "), "T extends ", Span("@", A("OnType"), "() "), A("Object"), ">",
                    ignoreSpanWithTokenStyle = true
                )

                val functionSignature = signatures[2]
                functionSignature.match(
                    Div(Div("@", A("OnMethod"), "()")),
                    "public final ", A("Boolean"), A("withParams"),
                    // <@OnTypeParameter() T extends @OnType() Object>
                    "<", Span("@", A("OnTypeParameter"), "() "), "T extends ", Span("@", A("OnType"), "() "), A("Any"), ">(",
                    Span(
                        Span(
                            Span("@", A("OnParameter"), "() "),
                            A("String"), "str1, "
                        ),
                        Span(
                            A("String"), "str2"
                        )
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}
