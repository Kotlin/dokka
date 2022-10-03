package signatures

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DefinitelyNonNullable
import org.jetbrains.dokka.model.dfs
import org.junit.jupiter.api.Test
import utils.*
import kotlin.test.assertEquals

class SignatureTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(
                    commonStdlibPath ?: throw IllegalStateException("Common stdlib is not found"),
                    jvmStdlibPath ?: throw IllegalStateException("JVM stdlib is not found")
                )
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    private val mppConfiguration = dokkaConfiguration {
        moduleName = "test"
        sourceSets {
            sourceSet {
                name = "common"
                sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
            sourceSet {
                name = "jvm"
                dependentSourceSets = setOf(DokkaSourceSetID("test", "common"))
                sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
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
    fun `fun`() {
        val source = source("fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `open fun`() {
        val source = source("open fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open fun ", A("simpleFun"), "(): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `open suspend fun`() {
        val source = source("open suspend fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open suspend fun ", A("simpleFun"), "(): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with params`() {
        val source = source("fun simpleFun(a: Int, b: Boolean, c: Any): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(", Parameters(
                        Parameter("a: ", A("Int"), ","),
                        Parameter("b: ", A("Boolean"), ","),
                        Parameter("c: ", A("Any")),
                    ), "): ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with function param`() {
        val source = source("fun simpleFun(a: (Int) -> String): String = \"Celebrimbor\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(", Parameters(
                        Parameter("a: (", A("Int"), ") -> ", A("String")),
                    ),"): ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with generic param`() {
        val source = source("fun <T> simpleFun(): T = \"Celebrimbor\" as T")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun <", A("T"), "> ", A("simpleFun"), "(): ",
                    A("T"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with generic bounded param`() {
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
                    "(): ", A("T"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with definitely non-nullable types`() {
        val source = source("fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = {
                val fn = (it.dfs { it.name == "elvisLike" } as? DFunction).assertNotNull("Function elvisLike")

                assert(fn.type is DefinitelyNonNullable)
                assert(fn.parameters[1].type is DefinitelyNonNullable)
            }
            renderingStage = { _, _ ->
                val signature = writerPlugin.writer.renderedContent("root/example/elvis-like.html")
                assertEquals(2, signature.select("a[href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html\"]").size)
                signature.firstSignature().match(
                    "fun <", A("T"), "> ", A("elvisLike"),
                    "(",
                    Span(
                        Span("x: ", A("T"), ", "),
                        Span("y: ", A("T"), " & ", A("Any"))
                    ),
                    "): ", A("T"), " & ", A("Any"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with keywords, params and generic bound`() {
        val source = source("inline suspend fun <T : String> simpleFun(a: Int, b: String): T = \"Celebrimbor\" as T")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "inline suspend fun <", A("T"), " : ", A("String"), "> ", A("simpleFun"), "(", Parameters(
                        Parameter("a: ", A("Int"), ","),
                        Parameter("b: ", A("String")),
                    ), "): ", A("T"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with vararg`() {
        val source = source("fun simpleFun(vararg params: Int): Unit")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", A("simpleFun"), "(", Parameters(
                        Parameter("vararg params: ", A("Int")),
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `class with no supertype`() {
        val source = source("class SimpleClass")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-simple-class/index.html").firstSignature().match(
                    "class ", A("SimpleClass"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `class with generic supertype`() {
        val source = source("class InheritingClassFromGenericType<T : Number, R : CharSequence> : Comparable<T>, Collection<R>")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-inheriting-class-from-generic-type/index.html").firstSignature().match(
                    "class ", A("InheritingClassFromGenericType"), " <", A("T"), " : ", A("Number"), ", ", A("R"), " : ", A("CharSequence"),
                    "> : ", A("Comparable"), "<", A("T"), "> , ", A("Collection"), "<", A("R"), ">",
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `functional interface`() {
        val source = source("fun interface KRunnable")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-k-runnable/index.html").firstSignature().match(
                    "fun interface ", A("KRunnable"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with annotation`() {
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
                        Div("@", A("Marking"))
                    ),
                    "fun ", A("simpleFun"),
                    "(): ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `property with annotation`() {
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | @MustBeDocumented()
            | @Target(AnnotationTarget.FUNCTION)
            | annotation class Marking
            |
            | @get:Marking()
            | @set:Marking()
            | var str: String
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/str.html").firstSignature().match(
                    Div(
                        Div("@get:", A("Marking")),
                        Div("@set:", A("Marking"))
                    ),
                    "var ", A("str"),
                    ": ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with two annotations`() {
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
                        "(): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @Test
    fun `fun with annotation with array`() {
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
                    "(): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `actual fun`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect fun simpleFun(): String
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |actual fun simpleFun(): String = "Celebrimbor"
                |
            """.trimMargin(),
            mppConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/simple-fun.html").signature().toList()

                signatures[0].match(
                    "expect fun ", A("simpleFun"),
                    "(): ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual fun ", A("simpleFun"),
                    "(): ", A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `actual property with a default value`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect val prop: Int
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |actual val prop: Int = 2
                |
            """.trimMargin(),
            mppConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/prop.html").signature().toList()

                signatures[0].match(
                    "expect val ", A("prop"),
                    ": ", A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual val ", A("prop"),
                    ": ", A("Int"),
                    " = 2",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `type with an actual typealias`() {
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
            mppConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/-foo/index.html").signature().toList()

                signatures[0].match(
                    "expect class ", A("Foo"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual typealias ", A("Foo"), " = ", A("Bar"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `plain typealias of plain class`() {

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias PlainTypealias = Int
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example.html").firstSignature().match(
                    "typealias ", A("PlainTypealias"), " = ", A("Int"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `plain typealias of plain class with annotation`() {

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |@MustBeDocumented
                |@Target(AnnotationTarget.TYPEALIAS)
                |annotation class SomeAnnotation
                |
                |@SomeAnnotation
                |typealias PlainTypealias = Int
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    Div(
                        Div(
                            "@", A("SomeAnnotation")
                        )
                    ),
                    "typealias ", A("PlainTypealias"), " = ", A("Int"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `plain typealias of generic class`() {

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias PlainTypealias = Comparable<Int>
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example.html").firstSignature().match(
                    "typealias ", A("PlainTypealias"), " = ", A("Comparable"),
                    "<", A("Int"), ">",
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `typealias with generics params`() {


        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias GenericTypealias<T> = Comparable<T>
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example.html").firstSignature().match(
                    "typealias ", A("GenericTypealias"), "<", A("T"), "> = ", A("Comparable"),
                    "<", A("T"), ">",
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `typealias with generic params swapped`() {

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |typealias XD<B, A> = Map<A, B>
            |
            |class ABC {
            |    fun someFun(xd: XD<Int, String>) = 1
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").firstSignature()
                    .match(
                        "fun ", A("someFun"), "(", Parameters(
                            Parameter("xd: ", A("XD"), "<", A("Int"), ", ", A("String"), ">"),
                        ), "):", A("Int"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `generic constructor params`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class GenericClass<T>(val x: Int) {
                |    constructor(x: T) : this(1)
                |
                |    constructor(x: Int, y: String) : this(1)
                |
                |    constructor(x: Int, y: List<T>) : this(1)
                |
                |    constructor(x: Boolean, y: Int, z: String) : this(1)
                |
                |    constructor(x: List<Comparable<Lazy<T>>>?) : this(1)
                |}
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic-class/-generic-class.html").signature().zip(
                    listOf(
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("T"))
                            ),
                            ")",
                        ),
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("Int"), ", "),
                                Parameter("y: ", A("String"))
                            ),
                            ")",
                        ),
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("Int"), ", "),
                                Parameter("y: ", A("List"), "<", A("T"), ">")
                            ),
                            ")",
                        ),
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("Boolean"), ", "),
                                Parameter("y: ", A("Int"), ", "),
                                Parameter("z:", A("String"))
                            ),
                            ")",
                        ),
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("List"), "<", A("Comparable"), "<", A("Lazy"), "<", A("T"), ">>>?")
                            ),
                            ")",
                        ),
                        arrayOf(
                            "constructor(",
                            Parameters(
                                Parameter("x: ", A("Int"))
                            ),
                            ")",
                        ),
                    )
                ).forEach {
                    it.first.match(*it.second, ignoreSpanWithTokenStyle = true)
                }
            }
        }
    }

    @Test
    fun `constructor has its own custom signature keyword in Constructor tab`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class PrimaryConstructorClass(x: String) { }
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val constructorTabFirstElement =
                    writerPlugin.writer.renderedContent("root/example/-primary-constructor-class/index.html")
                        .tab("Constructors")
                        .first() ?: throw NoSuchElementException("No Constructors tab found or it is empty")

                constructorTabFirstElement.firstSignature().match(
                    "constructor(", Parameters(Parameter("x: ", A("String"))), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `primary constructor with properties check for all tokens`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class PrimaryConstructorClass<T>(val x: Int, var s: String) { }
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-primary-constructor-class/index.html").firstSignature().match(
                    // In `<T>` expression, an empty `<span class="token keyword"></span>` is present for some reason
                    Span("class "), A("PrimaryConstructorClass"), Span("<"), Span(), A("T"), Span(">"), Span("("), Parameters(
                        Parameter(Span("val "), "x", Span(": "), A("Int"), Span(",")),
                        Parameter(Span("var "), "s", Span(": "), A("String"))
                    ), Span(")"),
                )
            }
        }
    }

    @Test
    fun `fun with default values`() {
        val source = source("fun simpleFun(int: Int = 1, string: String = \"string\"): String = \"\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun", A("simpleFun"), "(", Parameters(
                        Parameter("int: ", A("Int"), " = 1,"),
                        Parameter("string: ", A("String"), " = \"string\"")
                    ), "): ", A("String"),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `const val with default values`() {
        val source = source("const val simpleVal = 1")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "const val ", A("simpleVal"), ": ", A("Int"), " = 1",
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `should not expose enum constructor entry arguments`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
                |/src/main/kotlin/common/EnumClass.kt
                |package example
                |
                |enum class EnumClass(param: String = "Default") {
                |    EMPTY,
                |    WITH_ARG("arg")
                |}
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val enumEntrySignatures = writerPlugin.writer.renderedContent("root/example/-enum-class/index.html")
                    .select("div.table[data-togglable=Entries]")
                    .single()
                    .signature()
                    .select("div.block")

                enumEntrySignatures[0].match(
                    A("EMPTY"),
                    ignoreSpanWithTokenStyle = true
                )

                enumEntrySignatures[1].match(
                    A("WITH_ARG"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `java property without accessors should be var`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/test/JavaClass.java
            |package test;
            |public class JavaClass {
            |    public int property = 0;
            |}
            |
            |/src/test/KotlinClass.kt
            |package test
            |open class KotlinClass : JavaClass() { }
        """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected 2 signatures: class signature, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "var ", A("property"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(2, signatures.size, "Expected 2 signatures: class signature and property")

                    val property = signatures[1]
                    property.match(
                        "open var ", A("property"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }
}
