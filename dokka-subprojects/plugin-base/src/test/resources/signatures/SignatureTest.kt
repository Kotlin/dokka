/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.dfs
import org.jsoup.nodes.Element
import signatures.firstSignature
import signatures.lastSignature
import signatures.renderedContent
import signatures.signature
import signatures.tab
import utils.*
import utils.assertNotNull
import utils.match
import kotlin.collections.forEach
import kotlin.collections.single
import kotlin.collections.toList
import kotlin.collections.zip
import kotlin.let
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.trimIndent
import kotlin.text.trimMargin

class SignatureTest : org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest() {
    private val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                sourceRoots = kotlin.collections.listOf("src/")
                classpath = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                        ?: throw kotlin.IllegalStateException("Common stdlib is not found"),
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath
                        ?: throw kotlin.IllegalStateException("JVM stdlib is not found")
                )
                externalDocumentationLinks =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
            }
        }
    }

    private val mppConfiguration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.moduleName = "test"
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "common"
                sourceRoots = kotlin.collections.listOf("src/main/kotlin/common/Test.kt")
                classpath =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath!!)
                externalDocumentationLinks =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
            }
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "jvm"
                dependentSourceSets = kotlin.collections.setOf(org.jetbrains.dokka.DokkaSourceSetID("test", "common"))
                sourceRoots = kotlin.collections.listOf("src/main/kotlin/jvm/Test.kt")
                classpath = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                        ?: throw kotlin.IllegalStateException("Common stdlib is not found"),
                )
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
    fun `fun`() {
        val source = source("fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `open fun`() {
        val source = source("open fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open fun ", utils.A("simpleFun"), "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `open suspend fun`() {
        val source = source("open suspend fun simpleFun(): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "open suspend fun ", utils.A("simpleFun"), "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with params`() {
        val source = source("fun simpleFun(a: Int, b: Boolean, c: Any): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("a: ", utils.A("Int"), ","),
                        signatures.Parameter("b: ", utils.A("Boolean"), ","),
                        signatures.Parameter("c: ", utils.A("Any")),
                    ), "): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with function param`() {
        val source = source("fun simpleFun(a: (Int) -> String): String = \"Celebrimbor\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("a: (", utils.A("Int"), ") -> ", utils.A("String")),
                    ), "): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with generic param`() {
        val source = source("fun <T> simpleFun(): T = \"Celebrimbor\" as T")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun <", utils.A("T"), "> ", utils.A("simpleFun"), "(): ",
                    utils.A("T"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with generic bounded param`() {
        val source = source("fun <T : String> simpleFun(): T = \"Celebrimbor\" as T")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun <", utils.A("T"), " : ", utils.A("String"), "> ", utils.A("simpleFun"),
                    "(): ", utils.A("T"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with use site variance modifier in`() {
        val source = source("fun simpleFun(params: Array<in String>): Unit")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("params: ", utils.A("Array"), "<in ", utils.A("String"), ">"),
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with definitely non-nullable types`() {
        val source = source("fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            org.jetbrains.dokka.base.testApi.testRunner.BaseTestBuilder.documentablesTransformationStage = {
                val fn =
                    (it.dfs { it.name == "elvisLike" } as? org.jetbrains.dokka.model.DFunction).assertNotNull("Function elvisLike")

                kotlin.test.assertTrue(fn.type is org.jetbrains.dokka.model.DefinitelyNonNullable)
                kotlin.test.assertTrue(fn.parameters[1].type is org.jetbrains.dokka.model.DefinitelyNonNullable)
            }
            renderingStage = { _, _ ->
                val signature = writerPlugin.writer.renderedContent("root/example/elvis-like.html")
                kotlin.test.assertEquals(
                    2,
                    signature.select("a[href=\"https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html\"]").size
                )
                signature.firstSignature().match(
                    "fun <", utils.A("T"), "> ", utils.A("elvisLike"),
                    "(",
                    utils.Span(
                        utils.Span("x: ", utils.A("T"), ", "),
                        utils.Span("y: ", utils.A("T"), " & ", utils.A("Any"))
                    ),
                    "): ", utils.A("T"), " & ", utils.A("Any"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with keywords, params and generic bound`() {
        val source = source("inline suspend fun <T : String> simpleFun(a: Int, b: String): T = \"Celebrimbor\" as T")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "inline suspend fun <",
                    utils.A("T"),
                    " : ",
                    utils.A("String"),
                    "> ",
                    utils.A("simpleFun"),
                    "(",
                    signatures.Parameters(
                        signatures.Parameter("a: ", utils.A("Int"), ","),
                        signatures.Parameter("b: ", utils.A("String")),
                    ),
                    "): ",
                    utils.A("T"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function`() {
        val source = source("fun String.capitalizeAll(): String = toUpperCase()")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/capitalize-all.html").firstSignature()
                    .matchIgnoringSpans(
                        "fun", utils.A("String"), ".", utils.A("capitalizeAll"), "():",
                        utils.A("String")
                    )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with a param`() {
        val source = source("fun Int.addOneInt(a: Int): Int = this + a")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/add-one-int.html").firstSignature()
                    .matchIgnoringSpans(
                        "fun ", utils.A("Int"), ".", utils.A("addOneInt"), "(", signatures.Parameters(
                            signatures.Parameter("a: ", utils.A("Int")),
                        ), "): ", utils.A("Int")
                    )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with vararg`() {
        val source = source("fun Int.addAll(vararg ts: Int): Int = this + ts.sum()")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/add-all.html").firstSignature().matchIgnoringSpans(
                    "fun ", utils.A("Int"), ".", utils.A("addAll"), "(", signatures.Parameters(
                        signatures.Parameter("vararg ts: ", utils.A("Int"))
                    ), "): ", utils.A("Int")
                )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with generics`() {
        val source = source("fun <T> T.toList(vararg ts: T): List<T> = ts.asList()")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/to-list.html").firstSignature().matchIgnoringSpans(
                    "fun <", utils.A("T"), "> ", utils.A("T"), ".", utils.A("toList"), "(",
                    signatures.Parameters(
                        signatures.Parameter("vararg ts: ", utils.A("T"))
                    ), "): ", utils.A("List"), "<", utils.A("T"), ">"
                )
            }
        }
    }

    @kotlin.test.Test
    fun `infix function`() {
        val source = source("infix fun Int.eq(a: Int): Boolean = this==a")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/eq.html").firstSignature().matchIgnoringSpans(
                    "infix fun ", utils.A("Int"), ".", utils.A("eq"), "(", signatures.Parameters(
                        signatures.Parameter("a: ", utils.A("Int"))
                    ), "): ", utils.A("Boolean")
                )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with nullables`() {
        val source = source("fun String?.onDefault(default: String): String = this ?: default")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/on-default.html").firstSignature().matchIgnoringSpans(
                    "fun ", utils.A("String"), "?.", utils.A("onDefault"), "(", signatures.Parameters(
                        signatures.Parameter("default: ", utils.A("String"))
                    ), "): ", utils.A("String")
                )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with default args`() {
        val source = source("fun String.truncate(length: Int = 10): String = if (this.length > length) this.substring(0, length) else this")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/truncate.html").firstSignature().matchIgnoringSpans(
                    "fun ", utils.A("String"), ".", utils.A("truncate"), "(", signatures.Parameters(
                        signatures.Parameter("length: ", utils.A("Int"), " = 10")
                    ), "): ", utils.A("String")
                )
            }
        }
    }

    @kotlin.test.Test
    fun `extension function with lambda param`() {
        val source = source("fun <T> Iterable<T>.customForEach(action: (T) -> Unit) {for (element in this) action(element)}")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/custom-for-each.html").firstSignature()
                    .matchIgnoringSpans(
                        "fun <",
                        utils.A("T"),
                        ">",
                        utils.A("Iterable"),
                        "<",
                        utils.A("T"),
                        ">.",
                        utils.A("customForEach"),
                        "(",
                        signatures.Parameters(
                            signatures.Parameter("action: (", utils.A("T"), ") -> ", utils.A("Unit"))
                        ),
                        ")"
                    )
            }
        }
    }

    @kotlin.test.Test
    fun `property extension with nullables`() {
        val source = source("val String?.customLength: Int get() = this?.length ?: 0")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/custom-length.html").firstSignature()
                    .matchIgnoringSpans(
                        "val ", utils.A("String"), "?.", utils.A("customLength"), ": ", utils.A("Int")
                    )
            }
        }
    }

    @kotlin.test.Test
    @utils.OnlySymbols
    fun `fun with unresolved parameter`() {
        val source = source("fun simpleFun(param: UnresolvedType): Unit")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("param: UnresolvedType"),
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with vararg`() {
        val source = source("fun simpleFun(vararg params: Int): Unit")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun ", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("vararg params: ", utils.A("Int")),
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `class with no supertype`() {
        val source = source("class SimpleClass")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-simple-class/index.html").firstSignature().match(
                    "class ", utils.A("SimpleClass"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `class with generic supertype`() {
        val source = source("class InheritingClassFromGenericType<T : Number, R : CharSequence> : Comparable<T>, Collection<R>")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-inheriting-class-from-generic-type/index.html")
                    .firstSignature().match(
                    "class ",
                    utils.A("InheritingClassFromGenericType"),
                    " <",
                    utils.A("T"),
                    " : ",
                    utils.A("Number"),
                    ", ",
                    utils.A("R"),
                    " : ",
                    utils.A("CharSequence"),
                    "> : ",
                    utils.A("Comparable"),
                    "<",
                    utils.A("T"),
                    "> , ",
                    utils.A("Collection"),
                    "<",
                    utils.A("R"),
                    ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `class with declaration site variance modifier`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class PrimaryConstructorClass<out T> { }
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-primary-constructor-class/index.html")
                    .firstSignature().match(
                    utils.Span("class "),
                    utils.A("PrimaryConstructorClass"),
                    utils.Span("<"),
                    utils.Span("out "),
                    utils.A("T"),
                    utils.Span(">"),
                )
            }
        }
    }

    @kotlin.test.Test
    fun `kotlin sealed class should render sealed`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |sealed class Class
        """.trimMargin(),
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "sealed class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `kotlin abstract class should render abstract`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |abstract class Class
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "abstract class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `kotlin open class should render open`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |open class Class
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "open class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `kotlin final class should render just class`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |final class Class
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "class ", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `kotlin sealed interface should render sealed`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |sealed interface Interface
        """.trimMargin()
    ) {
        renderedContent("root/example/-interface/index.html").firstSignature().matchIgnoringSpans(
            "sealed interface", utils.A("Interface"),
        )
    }

    @kotlin.test.Test
    fun `kotlin interface should render just interface`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |interface Interface
        """.trimMargin()
    ) {
        renderedContent("root/example/-interface/index.html").firstSignature().matchIgnoringSpans(
            "interface", utils.A("Interface"),
        )
    }

    @kotlin.test.Test
    fun `kotlin abstract interface should render just interface`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |abstract interface Interface
        """.trimMargin()
    ) {
        renderedContent("root/example/-interface/index.html").firstSignature().matchIgnoringSpans(
            "interface", utils.A("Interface"),
        )
    }

    @kotlin.test.Test
    fun `kotlin enum should render just enum`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |enum class EnumClass { T }
        """.trimMargin()
    ) {
        renderedContent("root/example/-enum-class/index.html").firstSignature().matchIgnoringSpans(
            "enum", utils.A("EnumClass"), ":", utils.A("Enum"), "<", utils.A("EnumClass"), ">"
        )
    }

    @kotlin.test.Test
    fun `kotlin object should render just object`() = testRender(
        """
            |/src/main/kotlin/common/Test.kt
            |package example
            |object Obj
        """.trimMargin()
    ) {
        renderedContent("root/example/-obj/index.html").firstSignature().matchIgnoringSpans(
            "object", utils.A("Obj"),
        )
    }

    @kotlin.test.Test
    fun `java class should render open`() = testRender(
        """
            |/src/example/Class.java
            |package example;
            |public class Class {}
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "open class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `java final class should render just class`() = testRender(
        """
            |/src/example/Class.java
            |package example;
            |public final class Class {}
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `java abstract class should render abstract`() = testRender(
        """
            |/src/example/Class.java
            |package example;
            |public abstract class Class {}
        """.trimMargin()
    ) {
        renderedContent("root/example/-class/index.html").firstSignature().matchIgnoringSpans(
            "abstract class", utils.A("Class"),
        )
    }

    @kotlin.test.Test
    fun `java interface should render just interface`() = testRender(
        """
            |/src/example/Interface.java
            |package example;
            |public interface Interface {}
        """.trimMargin()
    ) {
        renderedContent("root/example/-interface/index.html").firstSignature().matchIgnoringSpans(
            "interface ", utils.A("Interface"),
        )
    }

    @kotlin.test.Test
    fun `java abstract interface should render just interface`() = testRender(
        """
            |/src/example/Interface.java
            |package example;
            |public abstract interface Interface {}
        """.trimMargin()
    ) {
        renderedContent("root/example/-interface/index.html").firstSignature().matchIgnoringSpans(
            "interface", utils.A("Interface"),
        )
    }

    @utils.OnlyJavaPsi
    @kotlin.test.Test
    fun `java enum should render just enum`() = testRender(
        """
            |/src/example/EnumClass.java
            |package example;
            |public enum EnumClass { T; }
        """.trimMargin()
    ) {
        renderedContent("root/example/-enum-class/index.html").firstSignature().matchIgnoringSpans(
            "enum", utils.A("EnumClass"),
        )
    }

    @kotlin.test.Test
    fun `constructor property on class page`() {
        val source = source("data class DataClass(val arg: String)")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                kotlin.test.assertEquals(
                    writerPlugin.writer.renderedContent("root/example/-data-class/index.html").lastSignature().html(),
                    "<span class=\"token keyword\">val </span><a href=\"arg.html\">arg</a><span class=\"token operator\">: </span><a href=\"https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html\">String</a>"

                )
            }
        }
    }

    @kotlin.test.Test
    fun `functional interface`() {
        val source = source("fun interface KRunnable { fun f(): Int }")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-k-runnable/index.html").firstSignature().match(
                    "fun interface ", utils.A("KRunnable"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
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
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    utils.Div(
                        utils.Div("@", utils.A("Marking"))
                    ),
                    "fun ", utils.A("simpleFun"),
                    "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
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
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/str.html").firstSignature().match(
                    utils.Div(
                        utils.Div("@get:", utils.A("Marking")),
                        utils.Div("@set:", utils.A("Marking"))
                    ),
                    "var ", utils.A("str"),
                    ": ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
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
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html")
                    .firstSignature()
                    .match(
                        utils.Div(
                            utils.Div(
                                "@",
                                utils.A("Marking"),
                                "(",
                                utils.Span("msg = ", utils.Span("\"Nenya\"")),
                                utils.Wbr,
                                ")"
                            ),
                            utils.Div(
                                "@",
                                utils.A("Marking2"),
                                "(",
                                utils.Span("int = ", utils.Span("1")),
                                utils.Wbr,
                                ")"
                            )
                        ),
                        "fun ", utils.A("simpleFun"),
                        "(): ", utils.A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @kotlin.test.Test
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
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    utils.Div(
                        utils.Div(
                            "@", utils.A("Marking"), "(", utils.Span(
                                "msg = [",
                                utils.Span(utils.Span("\"Nenya\""), ", "), utils.Wbr,
                                utils.Span(utils.Span("\"Vilya\""), ", "), utils.Wbr,
                                utils.Span(utils.Span("\"Narya\"")), utils.Wbr, "]"
                            ), utils.Wbr, ")"
                        )
                    ),
                    "fun ", utils.A("simpleFun"),
                    "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `actual fun`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures =
                    writerPlugin.writer.renderedContent("test/example/simple-fun.html").signature().toList()

                signatures[0].match(
                    "expect fun ", utils.A("simpleFun"),
                    "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual fun ", utils.A("simpleFun"),
                    "(): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `actual property with a default value`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/prop.html").signature().toList()

                signatures[0].match(
                    "expect val ", utils.A("prop"),
                    ": ", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual val ", utils.A("prop"),
                    ": ", utils.A("Int"),
                    " = 2",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }
    @kotlin.test.Test
    fun `actual typealias should have generic parameters and fully qualified name of the expansion type`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect class Array<T>
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |actual typealias Array<T> = kotlin.Array<T>
            """.trimMargin(),
            mppConfiguration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures =
                    writerPlugin.writer.renderedContent("test/example/-array/index.html").signature().toList()

                signatures[0].match(
                    "expect class ", utils.A("Array"), "<", utils.A("T"), ">",
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual typealias ",
                    utils.A("Array"),
                    "<",
                    utils.A("T"),
                    "> = ",
                    utils.A("kotlin.Array"),
                    "<",
                    utils.A("T"),
                    ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `type with an actual typealias`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures =
                    writerPlugin.writer.renderedContent("test/example/-foo/index.html").signature().toList()

                signatures[0].match(
                    "expect class ", utils.A("Foo"),
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual typealias ", utils.A("Foo"), " = ", utils.A("Bar"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `plain typealias of plain class`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias PlainTypealias = Int
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "typealias ", utils.A("PlainTypealias"), " = ", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `plain typealias of plain class with annotation`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    utils.Div(
                        utils.Div(
                            "@", utils.A("SomeAnnotation")
                        )
                    ),
                    "typealias ", utils.A("PlainTypealias"), " = ", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `plain typealias of generic class`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias PlainTypealias = Comparable<Int>
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "typealias ", utils.A("PlainTypealias"), " = ", utils.A("Comparable"),
                    "<", utils.A("Int"), ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `typealias with generics params`() {


        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |typealias GenericTypealias<T> = Comparable<T>
                |
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "typealias ", utils.A("GenericTypealias"), "<", utils.A("T"), "> = ", utils.A("Comparable"),
                    "<", utils.A("T"), ">",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `typealias with generic params swapped`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").firstSignature()
                    .match(
                        "fun ", utils.A("someFun"), "(", signatures.Parameters(
                            signatures.Parameter(
                                "xd: ",
                                utils.A("XD"),
                                "<",
                                utils.A("Int"),
                                ", ",
                                utils.A("String"),
                                ">"
                            ),
                        ), "):", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @utils.OnlyDescriptors("Order of constructors is different in K2")
    @kotlin.test.Test
    fun `generic constructor params`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-generic-class/-generic-class.html").signature().zip(
                    kotlin.collections.listOf(
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter("x: ", utils.A("T"))
                            ),
                            ")",
                        ),
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter("x: ", utils.A("Int"), ", "),
                                signatures.Parameter("y: ", utils.A("String"))
                            ),
                            ")",
                        ),
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter("x: ", utils.A("Int"), ", "),
                                signatures.Parameter("y: ", utils.A("List"), "<", utils.A("T"), ">")
                            ),
                            ")",
                        ),
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter("x: ", utils.A("Boolean"), ", "),
                                signatures.Parameter("y: ", utils.A("Int"), ", "),
                                signatures.Parameter("z:", utils.A("String"))
                            ),
                            ")",
                        ),
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter(
                                    "x: ",
                                    utils.A("List"),
                                    "<",
                                    utils.A("Comparable"),
                                    "<",
                                    utils.A("Lazy"),
                                    "<",
                                    utils.A("T"),
                                    ">>>?"
                                )
                            ),
                            ")",
                        ),
                        kotlin.arrayOf(
                            "constructor(",
                            signatures.Parameters(
                                signatures.Parameter("x: ", utils.A("Int"))
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

    @kotlin.test.Test
    fun `constructor has its own custom signature keyword in Constructor tab`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class PrimaryConstructorClass(x: String) { }
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val constructorTabFirstElement =
                    writerPlugin.writer.renderedContent("root/example/-primary-constructor-class/index.html")
                        .tab("CONSTRUCTOR")
                        .first() ?: throw kotlin.NoSuchElementException("No Constructors tab found or it is empty")

                constructorTabFirstElement.firstSignature().match(
                    "constructor(", signatures.Parameters(signatures.Parameter("x: ", utils.A("String"))), ")",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `primary constructor with properties check for all tokens`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |class PrimaryConstructorClass<T>(val x: Int, var s: String) { }
            """.trimMargin(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/-primary-constructor-class/index.html")
                    .firstSignature().match(
                    utils.Span("class "),
                    utils.A("PrimaryConstructorClass"),
                    utils.Span("<"),
                    utils.A("T"),
                    utils.Span(">"),
                    utils.Span("("),
                    signatures.Parameters(
                        signatures.Parameter(
                            utils.Span("val "),
                            "x",
                            utils.Span(": "),
                            utils.A("Int"),
                            utils.Span(",")
                        ),
                        signatures.Parameter(utils.Span("var "), "s", utils.Span(": "), utils.A("String"))
                    ),
                    utils.Span(")"),
                )
            }
        }
    }

    @kotlin.test.Test
    fun `fun with default values`() {
        val source = source("fun simpleFun(int: Int = 1, string: String = \"string\"): String = \"\"")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "fun", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("int: ", utils.A("Int"), " = 1,"),
                        signatures.Parameter("string: ", utils.A("String"), " = \"string\"")
                    ), "): ", utils.A("String"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    @utils.OnlySymbols("context parameters")
    @OptIn(org.jetbrains.dokka.ExperimentalDokkaApi::class)
    fun `fun with context parameters`() {
        val source = source("""
            context(s: String, _:Int)
            fun Int.simpleFun(a: Int): String = \"\""
        """.trimIndent())
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-fun.html").firstSignature().match(
                    "context(", signatures.ContextParameters(
                        signatures.Parameter("s: ", utils.A("String"), ", "),
                        signatures.Parameter("_: ", utils.A("Int")),
                    ), ")", utils.Br, "fun ", utils.A("Int"), ".", utils.A("simpleFun"), "(", signatures.Parameters(
                        signatures.Parameter("a: ", utils.A("Int")),
                    ), "): ", utils.A("String"), ignoreSpanWithTokenStyle = true
                )
            }
        }
    }


    @kotlin.test.Test
    @utils.OnlySymbols("context parameters")
    @OptIn(org.jetbrains.dokka.ExperimentalDokkaApi::class)
    fun `property with context parameters`() {
        val source = source("""
            context(s: String, _:Int) val Int.simpleProp : Int
        """.trimIndent())
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/simple-prop.html").firstSignature().match(
                    "context(", signatures.ContextParameters(
                        signatures.Parameter("s: ", utils.A("String"), ", "),
                        signatures.Parameter("_: ", utils.A("Int")),
                    ), ")", utils.Br, "val ", utils.A("Int"), ".", utils.A("simpleProp"), ": ", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `const val with default values`() {
        val source = source("const val simpleVal = 1")
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            source,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/example/index.html").firstSignature().match(
                    "const val ", utils.A("simpleVal"), ": ", utils.A("Int"), " = 1",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @kotlin.test.Test
    fun `should not expose enum constructor entry arguments`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val enumEntrySignatures = writerPlugin.writer.renderedContent("root/example/-enum-class/index.html")
                    .select("div[data-togglable=ENTRY] .table")
                    .single()
                    .signature()
                    .select("div.block")

                enumEntrySignatures[0].match(
                    utils.A("EMPTY"),
                    ignoreSpanWithTokenStyle = true
                )

                enumEntrySignatures[1].match(
                    utils.A("WITH_ARG"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `java property without accessors should be var`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 2 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "var ", utils.A("property"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open var ", utils.A("property"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @kotlin.test.Test
    fun `should not add an empty span with java default visibility`() {
        val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
            testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
                testApi.testRunner.SourceSetsBuilder.sourceSet {
                    sourceRoots = kotlin.collections.listOf("src/")
                    documentedVisibilities = kotlin.collections.setOf(
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PACKAGE
                    )
                }
            }
        }

        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/JavaAnnotationWithSpace.java
            |package test;
            |
            |@interface JavaAnnotationWithSpace {}
        """.trimIndent(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatureHtml =
                    writerPlugin.writer.renderedContent("root/test/-java-annotation-with-space/index.html")
                        .firstSignature()
                        .html()

                val expectedSignature =
                    "<span class=\"token keyword\">annotation class </span><a href=\"index.html\">JavaAnnotationWithSpace</a>"

                kotlin.test.assertEquals(expectedSignature, signatureHtml)
            }
        }
    }

    @kotlin.test.Test
    fun `primary constructor parameter should not be marked as property for derived generic class`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |abstract class Parent<out RowType : Any>(val name: (String) -> RowType)
            |abstract class Child<out RowType : Any>(name: (String) -> RowType) : Parent<RowType>(name)
        """.trimMargin(),
    ) {
        renderedContent("root/[root]/-child/index.html").firstSignature().matchIgnoringSpans(
            "abstract class",
            utils.A("Child"), "<out",
            utils.A("RowType"), " : ",
            utils.A("Any"), ">(",
            signatures.Parameters(
                signatures.Parameter("name: (", utils.A("String"), ") -> ", utils.A("RowType"))
            ), ") : ",
            utils.A("Parent"), "<",
            utils.A("RowType"), "> "
        )
    }

    @kotlin.test.Test
    @utils.OnlySymbols("#4056")
    fun `primary constructor parameter should not be marked as property for derived non-generic class`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |abstract class Parent(val name: (String) -> Int)
            |abstract class Child(name: (String) -> Int) : Parent
        """.trimMargin(),
    ) {
        renderedContent("root/[root]/-child/index.html").firstSignature().matchIgnoringSpans(
            "abstract class", utils.A("Child"), "(", signatures.Parameters(
                signatures.Parameter("name: (", utils.A("String"), ") -> ", utils.A("Int"))
            ), ") : ", utils.A("Parent")
        )
    }

    @kotlin.test.Test
    fun `primary constructor parameter should not be marked as property`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |abstract class Parent(val name: (String) -> Int)
            |abstract class Child(name: (String) -> Int) : Parent {
            |   override val name: (String) -> Int = name
            |}
        """.trimMargin(),
    ) {
        renderedContent("root/[root]/-child/index.html").firstSignature().matchIgnoringSpans(
            "abstract class", utils.A("Child"), "(", signatures.Parameters(
                signatures.Parameter("name: (", utils.A("String"), ") -> ", utils.A("Int"))
            ), ") : ", utils.A("Parent")
        )
    }

    @kotlin.test.Test
    fun `should render actual keyword for constructor`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect class A()
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |actual class A{
                |    actual constructor(){}
                |}
            """.trimMargin(),
            mppConfiguration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("test/example/-a/-a.html").signature().toList()

                signatures[0].match(
                    "expect constructor()",
                    ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "actual constructor()",
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @utils.OnlyDescriptors("#3354")
    @kotlin.test.Test
    fun `should not render parameterless constructor with annotation without mustBeDocumented annotation - for kotlin Any `() = testRender(
        """
            |/src/main/kotlin/Any.kt
            |package kotlin
            |annotation class WasmPrimitiveConstructor
            |open class Any @WasmPrimitiveConstructor constructor()
        """.trimMargin(),
    ) {
        renderedContent("root/kotlin/-any/index.html").firstSignature().matchIgnoringSpans(
            "open class", utils.A("Any")
        )
    }

    @kotlin.test.Test
    fun `should not render parameterless constructor with annotation without mustBeDocumented annotation`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |package example
            |annotation class SomeAnnotation
            |class SomeClass @SomeAnnotation constructor()
        """.trimMargin(),
    ) {
        renderedContent("root/example/-some-class/index.html").firstSignature().matchIgnoringSpans(
            "class", utils.A("SomeClass")
        )
    }

    @kotlin.test.Test
    fun `should not render parameterless constructor with ignored annotation`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |package example
            |class SomeClass @Deprecated("reason") constructor()
        """.trimMargin(),
    ) {
        renderedContent("root/example/-some-class/index.html").firstSignature().matchIgnoringSpans(
            "class", utils.A("SomeClass")
        )
    }

    @kotlin.test.Test
    fun `should render parameterless constructor with annotation with mustBeDocumented annotation`() = testRender(
        """
            |/src/main/kotlin/SomeClass.kt
            |package example
            |@MustBeDocumented
            |annotation class SomeAnnotation
            |class SomeClass @SomeAnnotation constructor()
        """.trimMargin(),
    ) {
        renderedContent("root/example/-some-class/index.html").firstSignature().matchIgnoringSpans(
            "class", utils.A("SomeClass"), utils.Span("@", utils.A("SomeAnnotation")), "constructor"
        )
    }

    @kotlin.test.Test
    fun `fun and prop should have external modifier`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
            testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
                testApi.testRunner.SourceSetsBuilder.sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    classpath = kotlin.collections.listOf(
                        org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                            ?: throw kotlin.IllegalStateException("Common stdlib is not found")
                    )
                    sourceRoots = kotlin.collections.listOf("src/main/kotlin/js/Test.kt")
                    externalDocumentationLinks =
                        kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
                }
            }
        }

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/main/kotlin/js/Test.kt
            |package multiplatform
            |
            |external fun fn(): Unit
            |external val x: String
        """.trimMargin(), configuration = configuration, pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures =
                    writerPlugin.writer.renderedContent("root/multiplatform/index.html").signature().toList()
                signatures[0].match(
                    "external val ", utils.A("x"), ": ", utils.A("String"), ignoreSpanWithTokenStyle = true
                )
                signatures[1].match(
                    "external fun", utils.A("fn"), "()", ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    private fun testRender(
        query: String,
        configuration: org.jetbrains.dokka.DokkaConfigurationImpl = this.configuration,
        block: utils.TestOutputWriter.() -> Unit
    ) {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            query,
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ -> writerPlugin.writer.block() }
        }
    }

    private fun org.jsoup.nodes.Element.matchIgnoringSpans(vararg matchers: Any) {
        return match(*matchers, ignoreSpanWithTokenStyle = true)
    }
}
