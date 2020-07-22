package signatures

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import utils.*

class SignatureTest : AbstractCoreTest() {

    fun source(signature: String) =
        """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | $signature
            """.trimIndent()

    @Test
    fun `fun`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `open fun`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("open fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open fun ", A("simpleFun"), "(): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `open suspend fun`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("open suspend fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open suspend fun ", A("simpleFun"), "(): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with params`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("fun simpleFun(a: Int, b: Boolean, c: Any): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(a: ", A("Int"),
                    ", b: ", A("Boolean"), ", c: ", A("Any"),
                    "): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with function param`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("fun simpleFun(a: (Int) -> String): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(a: (", A("Int"),
                    ") -> ", A("String"), "): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with generic param`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("fun <T> simpleFun(): T = \"Celebrimbor\" as T")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun <", A("T"), " : ", A("Any"), "?> ", A("simpleFun"), "(): ",
                    A("T"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with generic bounded param`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("fun <T : String> simpleFun(): T = \"Celebrimbor\" as T")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun <", A("T"), " : ", A("String"), "> ", A("simpleFun"),
                    "(): ", A("T"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with keywords, params and generic bound`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = source("inline suspend fun <T : String> simpleFun(a: Int, b: String): T = \"Celebrimbor\" as T")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "inline suspend fun <", A("T"), " : ", A("String"), "> ", A("simpleFun"),
                    "(a: ", A("Int"), ", b: ", A("String"), "): ", A("T"), Span()
                )
            }
        }
    }


    @Test
    fun `fun with annotation`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | @MustBeDocumented()
            | @Target(AnnotationTarget.FUNCTION)
            | annotation class Marking
            |
            | @Marking()
            | fun simpleFun(): String = "Celebrimbor"
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    Div(
                        Div("@", A("Marking"), "()")
                    ),
                    "fun ", A("simpleFun"),
                    "(): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `fun with two annotations`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | @MustBeDocumented()
            | @Target(AnnotationTarget.FUNCTION)
            | annotation class Marking(val msg: String)
            |
            | @MustBeDocumented()
            | @Target(AnnotationTarget.FUNCTION)
            | annotation class Marking2(val int: Int)
            |
            | @Marking("Nenya")
            | @Marking2(1)
            | fun simpleFun(): String = "Celebrimbor"
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html")
                    .firstSignature()
                    .match(
                        Div(
                            Div("@", A("Marking"), "(", Span("msg = ", Span("\"Nenya\"")), Wbr, ")"),
                            Div("@", A("Marking2"), "(", Span("int = ", Span("1")), Wbr, ")")
                        ),
                        "fun ", A("simpleFun"),
                        "(): ", A("String"), Span()
                    )
            }
        }
    }

    @Test
    fun `fun with annotation with array`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }

        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | @MustBeDocumented()
            | @Target(AnnotationTarget.FUNCTION)
            | annotation class Marking(val msg: Array<String>)
            |
            | @Marking(["Nenya", "Vilya", "Narya"])
            | @Marking2(1)
            | fun simpleFun(): String = "Celebrimbor"
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    Div(
                        Div(
                            "@", A("Marking"), "(", Span(
                                "msg = [",
                                Span(Span("\"Nenya\""), ", "), Wbr,
                                Span(Span("\"Vilya\""), ", "), Wbr,
                                Span(Span("\"Narya\"")), Wbr, "]"
                            ), Wbr, ")"
                        )
                    ),
                    "fun ", A("simpleFun"),
                    "(): ", A("String"), Span()
                )
            }
        }
    }

    @Test
    fun `type with an actual typealias`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    moduleName = "test"
                    name = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                }
                sourceSet {
                    moduleName = "test"
                    name = "jvm"
                    dependentSourceSets = setOf(DokkaSourceSetID("test", "common"))
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect class Foo
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |class Bar
                |actual typealias Foo = Bar
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("test/example/-foo/index.html").signature().toList()[1].match(
                    "typealias ", A("Foo"), " = ", A("Bar"), Span()
                )
            }
        }
    }

    private fun TestOutputWriter.renderedContent(path: String = "root/example.html") =
        contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
            .single()

    private fun Element.signature() = select("div.symbol.monospace")
    private fun Element.firstSignature() = signature().first()
}