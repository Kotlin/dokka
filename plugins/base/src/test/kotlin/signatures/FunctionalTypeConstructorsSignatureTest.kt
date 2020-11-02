package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.jdk
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import utils.A
import utils.Span
import utils.TestOutputWriterPlugin
import utils.match

class FunctionalTypeConstructorsSignatureTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(
                    stdlibExternalDocumentationLink,
                    DokkaConfiguration.ExternalDocumentationLink.Companion.jdk(8)
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

    @Test
    fun `kotlin normal function`() {
        val source = source("val nF: Function1<Int, String> = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": (", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar function`() {
        val source = source("val nF: (Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": (", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar extension function`() {
        val source = source("val nF: Boolean.(Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": ", A("Boolean"), ".(", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar function with param name`() {
        val source = source("val nF: (param: Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": (param: ", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Disabled // Add coroutines on classpath and get proper import
    @Test
    fun `kotlin normal suspendable function`() {
        val source = source("val nF: SuspendFunction1<Int, String> = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": suspend (", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar suspendable function`() {
        val source = source("val nF: suspend (Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": suspend (", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar suspendable extension function`() {
        val source = source("val nF: suspend Boolean.(Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": suspend ", A("Boolean"), ".(", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar suspendable function with param name`() {
        val source = source("val nF: suspend (param: Int) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ", A("nF"), ": suspend (param: ", A("Int"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `kotlin syntactic sugar suspendable fancy function with param name`() {
        val source =
            source("val nF: suspend (param1: suspend Boolean.(param2: List<Int>) -> Boolean) -> String = { _ -> \"\" }")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "val ",
                    A("nF"),
                    ": suspend (param1: suspend",
                    A("Boolean"),
                    ".(param2: ",
                    A("List"),
                    "<",
                    A("Int"),
                    ">) -> ",
                    A("Boolean"),
                    ") -> ",
                    A("String"),
                    Span()
                )
            }
        }
    }

    @Test
    fun `java with java function`() {
        val source = """
            |/src/main/kotlin/test/JavaClass.java
            |package example
            |
            |public class JavaClass {
            |    public java.util.function.Function<Integer, String> javaFunction = null;
            |}
        """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-java-class/index.html").signature().last().match(
                    "open val ", A("javaFunction"), ": (", A("Integer"), ") -> ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `java with kotlin function`() {
        val source = """
            |/src/main/kotlin/test/JavaClass.java
            |package example
            |
            |public class JavaClass {
            |    public kotlin.jvm.functions.Function1<Integer, String> kotlinFunction = null;
            |}
        """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-java-class/index.html").signature().last().match(
                    "open val ", A("kotlinFunction"), ": (", A("Integer"), ") -> ", A("String"), Span()
                )
            }
        }
    }
}
