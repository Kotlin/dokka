package signatures

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import utils.*
import kotlin.test.assertFalse

class SignatureTest : BaseAbstractTest() {
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
                    "fun ", A("simpleFun"), "(): ", A("String"), Span(),
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
                    "open fun ", A("simpleFun"), "(): ", A("String"), Span(),
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
                    "open suspend fun ", A("simpleFun"), "(): ", A("String"), Span(),
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
                    ), "): ", A("String"), Span(),
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
                    ),"): ", A("String"), Span(),
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
                    A("T"), Span(),
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
                    "(): ", A("T"), Span(),
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
                    ), "): ", A("T"), Span(),
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
                    ), ")", Span(),
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
                    "class ", A("SimpleClass"), Span(),
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
                    "> : ", A("Comparable"), "<", A("T"), "> , ", A("Collection"), "<", A("R"), ">", Span(),
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
                    "fun interface ", A("KRunnable"), Span(),
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
                    "(): ", A("String"), Span(),
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
                    ": ", A("String"), Span(),
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
                        "(): ", A("String"), Span(),
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
                    "(): ", A("String"), Span(),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `actual fun`() {

        val configuration = dokkaConfiguration {
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
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/simple-fun.html").signature().toList()

                signatures[0].match(
                    "expect fun ", A("simpleFun"),
                    "(): ", A("String"), Span(),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual fun ", A("simpleFun"),
                    "(): ", A("String"), Span(),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `type with an actual typealias`() {

        val configuration = dokkaConfiguration {
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
                val signatures = writerPlugin.writer.renderedContent("test/example/-foo/index.html").signature().toList()

                signatures[0].match(
                    "expect class ", A("Foo"), Span(),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual typealias ", A("Foo"), " = ", A("Bar"), Span(),
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
                writerPlugin.writer.renderedContent("root/example.html").signature().first().match(
                    "typealias ", A("PlainTypealias"), " = ", A("Int"), Span(),
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
                writerPlugin.writer.renderedContent("root/example/index.html").signature().first().match(
                    Div(
                        Div(
                            "@", A("SomeAnnotation")
                        )
                    ),
                    "typealias ", A("PlainTypealias"), " = ", A("Int"), Span(),
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
                writerPlugin.writer.renderedContent("root/example.html").signature().first().match(
                    "typealias ", A("PlainTypealias"), " = ", A("Comparable"),
                    "<", A("Int"), ">", Span(),
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
                writerPlugin.writer.renderedContent("root/example.html").signature().first().match(
                    "typealias ", A("GenericTypealias"), "<", A("T"), "> = ", A("Comparable"),
                    "<", A("T"), ">", Span(),
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
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").signature().first()
                    .match(
                        "fun ", A("someFun"), "(", Parameters(
                            Parameter("xd: ", A("XD"), "<", A("Int"), ", ", A("String"), ">"),
                        ), "):", A("Int"), Span(),
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
                            "fun <", A("T"), "> ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("T"))
                            ), ")", Span()
                        ),
                        arrayOf(
                            "fun ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("Int"), ", "),
                                Parameter("y: ", A("String"))
                            ), ")", Span()
                        ),
                        arrayOf(
                            "fun <", A("T"), "> ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("Int"), ", "),
                                Parameter("y: ", A("List"), "<", A("T"), ">")
                            ), ")", Span()
                        ),
                        arrayOf(
                            "fun ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("Boolean"), ", "),
                                Parameter("y: ", A("Int"), ", "),
                                Parameter("z:", A("String"))
                            ), ")", Span()
                        ),
                        arrayOf(
                            "fun <", A("T"), "> ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("List"), "<", A("Comparable"), "<", A("Lazy"), "<", A("T"), ">>>?")
                            ), ")", Span()
                        ),
                        arrayOf(
                            "fun ", A("GenericClass"), "(", Parameters(
                                Parameter("x: ", A("Int"))
                            ), ")", Span()
                        ),
                    )
                ).forEach {
                    it.first.match(*it.second, ignoreSpanWithTokenStyle = true)
                }
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
                    ), Span(")"), Span(),
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
                    ), "): ", A("String"), Span(),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `fun with single param should NOT have any wrapped or indented parameters`() {
        val source = source("fun assertNoIndent(int: Int): String = \"\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signature = writerPlugin.writer.renderedContent("root/example/assert-no-indent.html").firstSignature()
                signature.match(
                    "fun", A("assertNoIndent"), "(", Parameters(
                        Parameter("int: ", A("Int")),
                    ), "): ", A("String"), Span(),
                    ignoreSpanWithTokenStyle = true
                )
                assertFalse { signature.select("span.parameters").single().hasClass("wrapped") }
                assertFalse { signature.select("span.parameters > span.parameter").single().hasClass("indented") }
            }
        }
    }

    @Test
    fun `fun with many params should have wrapped and indented parameters`() {
        val source = source("fun assertParamsIndent(int: Int, string: String, long: Long): String = \"\"")
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/assert-params-indent.html").firstSignature().match(
                    "fun", A("assertParamsIndent"), "(", Parameters(
                        Parameter("int: ", A("Int"), ",").withClasses("indented"),
                        Parameter("string: ", A("String"), ",").withClasses("indented"),
                        Parameter("long: ", A("Long")).withClasses("indented")
                    ).withClasses("wrapped"), "): ", A("String"), Span(),
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
                    "const val ", A("simpleVal"), ": ", A("Int"), " = 1", Span(),
                        ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
}
