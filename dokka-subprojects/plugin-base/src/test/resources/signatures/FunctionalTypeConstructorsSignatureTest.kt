/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.jdk
import signatures.firstSignature
import signatures.lastSignature
import signatures.renderedContent
import utils.A
import utils.OnlyJavaPsi
import utils.OnlySymbols
import utils.Span
import utils.TestOutputWriterPlugin
import utils.Wbr
import utils.match
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.text.trimIndent

class FunctionalTypeConstructorsSignatureTest : org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest() {
    private val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                sourceRoots = kotlin.collections.listOf("src/")
                classpath = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath!!,
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath!!
                )
                externalDocumentationLinks = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink,
                    org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Companion.jdk(8)
                )
            }
        }
    }

    private val jvmConfiguration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                sourceRoots = kotlin.collections.listOf("src/")
                classpath = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath
                        ?: throw kotlin.IllegalStateException("JVM stdlib is not found")
                )
                externalDocumentationLinks = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink,
                    org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Companion.jdk(8)
                )
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
    fun `kotlin normal function`() {
        val source = source("val nF: Function1<Int, String> = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": (", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar function`() {
        val source = source("val nF: (Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": (", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar extension function`() {
        val source = source("val nF: Boolean.(Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": ", utils.A("Boolean"), ".(", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar function with param name`() {
        val source = source("val nF: (param: Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            org.jetbrains.dokka.base.testApi.testRunner.BaseTestBuilder.documentablesMergingStage = {
                kotlin.io.println(it)
            }
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": (param: ", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar function with explicit the @ParameterName annotation`() {
        val source = source("val nF:  (@ParameterName(name=\"param\") Int) -> String = { _ -> \"\" }\n")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ",
                    utils.A("nF"),
                    ": (param: ",
                    utils.Span(utils.Wbr, ") "),
                    utils.A("Int"),
                    ") -> ",
                    utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar function with param name of generic and functional type`() {
        val source = source("""
                            | @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
                            | @MustBeDocumented
                            | annotation class Fancy
                            |
                            | fun <T> f(): (param1: T, param2: @Fancy ()->Unit) -> String "
                            """.trimIndent())
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source, configuration, pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").lastSignature().match(
                    "fun <", utils.A("T"), "> ",
                    utils.A("f"), "(): (param1:", utils.A("T"),
                    ", param2: ", utils.Span("@", utils.A("Fancy")), " () -> ", utils.A("Unit"),
                    ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
    @kotlin.test.Ignore // Add coroutines on classpath and get proper import
    @kotlin.test.Test
    fun `kotlin normal suspendable function`() {
        val source = source("val nF: SuspendFunction1<Int, String> = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": suspend (", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar suspendable function`() {
        val source = source("val nF: suspend (Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": suspend (", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar suspendable extension function`() {
        val source = source("val nF: suspend Boolean.(Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ",
                    utils.A("nF"),
                    ": suspend ",
                    utils.A("Boolean"),
                    ".(",
                    utils.A("Int"),
                    ") -> ",
                    utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar suspendable function with param name`() {
        val source = source("val nF: suspend (param: Int) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", utils.A("nF"), ": suspend (param: ", utils.A("Int"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin syntactic sugar suspendable fancy function with param name`() {
        val source =
            source("val nF: suspend (param1: suspend Boolean.(param2: List<Int>) -> Boolean) -> String = { _ -> \"\" }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ",
                    utils.A("nF"),
                    ": suspend (param1: suspend",
                    utils.A("Boolean"),
                    ".(param2: ",
                    utils.A("List"),
                    "<",
                    utils.A("Int"),
                    ">) -> ",
                    utils.A("Boolean"),
                    ") -> ",
                    utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @utils.OnlyJavaPsi
    @kotlin.test.Test
    fun `java with java function`() {
        val source = """
            |/src/main/kotlin/test/JavaClass.java
            |package example
            |
            |public class JavaClass {
            |    public java.util.function.Function<Integer, String> javaFunction = null;
            |}
        """.trimIndent()
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-java-class/index.html").lastSignature().match(
                    "open var ", utils.A("javaFunction"), ": (", utils.A("Integer"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @utils.OnlyJavaPsi
    @kotlin.test.Test
    fun `java with kotlin function`() {
        val source = """
            |/src/main/kotlin/test/JavaClass.java
            |package example
            |
            |public class JavaClass {
            |    public kotlin.jvm.functions.Function1<Integer, String> kotlinFunction = null;
            |}
        """.trimIndent()
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            jvmConfiguration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-java-class/index.html").lastSignature().match(
                    "open var ", utils.A("kotlinFunction"), ": (", utils.A("Integer"), ") -> ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    @utils.OnlySymbols("context parameters")
    fun `lambda with context parameters`() {
        val source = source("fun simpleFun(a: context(String, Double) Boolean.(Int) -> String): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter(
                            "a: context(",
                            utils.A("String"),
                            ", ",
                            utils.A("Double"),
                            ") ",
                            utils.A("Boolean"),
                            ".(",
                            utils.A("Int"),
                            ") -> ",
                            utils.A("String")
                        ),
                    ), "): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}
