/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinAsJavaPlugin

import kotlinAsJavaPlugin.utils.OnlyJavaPsi
import matchers.content.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.jdk
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.GenericTypeConstructor
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.jsoup.nodes.Element
import signatures.Parameter
import signatures.Parameters
import signatures.firstSignature
import signatures.renderedContent
import utils.A
import utils.TestOutputWriterPlugin
import utils.match
import java.net.URL
import kotlin.test.*

class KotlinAsJavaPluginTest : BaseAbstractTest() {
    val defaultConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }
    val stdlibConfiguration = dokkaConfiguration {
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

    @Test
    fun `top-level functions should be generated`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |object TestObj {}
            |
            |fun testFL(l: List<String>) = l
            |fun testF() {}
            |fun testF2(i: Int) = i
            |fun testF3(to: TestObj) = to
            |fun <T : Char> testF4(t: T) = listOf(t)
            |val testV = 1
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = (root.children.single().children.first { it.name == "TestKt" } as ContentPage).content

                val functionRows = content.findTableWithKind(kind = ContentKind.Functions).children
                functionRows.assertCount(6)

                val propRows = content.findTableWithKind(kind = ContentKind.Properties).children
                propRows.assertCount(1)
            }
        }
    }

    private fun ContentNode.findTableWithKind(kind: ContentKind): ContentNode = dfs { node ->
        node is ContentTable && node.dci.kind === kind
    }.let { assertNotNull(it, "the table with kind $kind") }

    @Test
    fun topLevelWithClassTest() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test {
            |   fun testFC() {}
            |   val testVC = 1
            |}
            |
            |fun testF(i: Int) = i
            |val testV = 1
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val contentList = root.children
                    .flatMap { it.children<ContentPage>() }

                contentList.find { it.name == "Test" }.apply {
                    assertNotNull(this)
                    content.findTableWithKind(ContentKind.Functions).children.assertCount(2)
                    content.findTableWithKind(ContentKind.Properties).children.assertCount(1)
                }
                contentList.find { it.name == "TestKt" }.apply {
                    assertNotNull(this)
                    content.findTableWithKind(ContentKind.Functions).children.assertCount(2)
                    content.findTableWithKind(ContentKind.Properties).children.assertCount(1)
                }
            }
        }
    }

    @Test
    fun kotlinAndJavaTest() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |fun testF(i: Int) = i
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin;
            |
            |public class TestJ {
            |   public int testF(int i) { return i; }
            |}
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val classes = root.children.first().children.associateBy { it.name }
                classes.values.assertCount(2, "Class count: ")

                classes["TestKt"].let {
                    it?.children.orEmpty().assertCount(1, "(Kotlin) TestKt members: ")
                    it!!.children.first()
                        .let { assertEquals("testF", it.name, "(Kotlin) Expected method name: testF, got: ${it.name}") }
                }

                classes["TestJ"].let {
                    it?.children.orEmpty().assertCount(2, "(Java) TestJ members: ") // constructor + method
                    it!!.children.map { it.name }
                        .let {
                            assertTrue(
                                it.containsAll(
                                    setOf(
                                        "testF",
                                        "TestJ"
                                    )
                                ),
                                "(Java) Expected method name: testF, got: $it"
                            )
                        }
                }
            }
        }
    }

    @Test
    fun `public kotlin properties should have a getter with same visibilities`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test {
            |   public val publicProperty: String = ""
            |}
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesTransformationStage = { rootPageNode ->
                val propertyGetter =
                    rootPageNode.dfs { it is MemberPageNode && it.name == "getPublicProperty" } as? MemberPageNode
                assertNotNull(propertyGetter)
                propertyGetter.content.assertNode {
                    group {
                        header(1) {
                            +"getPublicProperty"
                        }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                group {
                                    +"public final "
                                    group {
                                        link {
                                            +"String"
                                        }
                                    }
                                    link {
                                        +"getPublicProperty"
                                    }
                                    +"()"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: AA renders Java property modifiers differently — likely accessor convention issue
    @OnlyJavaPsi
    @Test
    fun `java properties should keep its modifiers`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin;
            |
            |public class TestJ {
            |   public Int publicProperty = 1;
            |}
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "TestJ" } as? ClasslikePageNode
                assertNotNull(testClass)
                (testClass.content as ContentGroup).children.last().children.last().assertNode {
                    group {
                        header(2) {
                            +"Properties"
                        }
                        table {
                            group {
                                link {
                                    +"publicProperty"
                                }
                                divergentGroup {
                                    divergentInstance {
                                        divergent {
                                            group {
                                                group {
                                                    +"public Int"
                                                    link {
                                                        +"publicProperty"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `koltin interfaces and classes should be split to extends and implements`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |open class A { }
            |interface B
            |class C : A(), B
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "C" } as? ClasslikePageNode
                assertNotNull(testClass)
                testClass.content.assertNode {
                    group {
                        header(expectedLevel = 1) {
                            +"C"
                        }
                        platformHinted {
                            group {
                                +"public final class "
                                link {
                                    +"C"
                                }
                                +" extends "
                                group {
                                    link {
                                        +"A"
                                    }
                                }
                                +" implements "
                                group {
                                    link {
                                        +"B"
                                    }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assertEquals(n, count(), "${prefix}Expected $n, got ${count()}")

    @Test
    fun `typealias`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |typealias XD = Int
            |class ABC {
            |    fun someFun(xd: XD): Int = 1
            |}
        """.trimMargin(),
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").firstSignature()
                    .match(
                        "public final ", A("Integer"), A("someFun"), "(", Parameters(
                            Parameter(A("Integer"), "xd")
                        ), ")", ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @Test
    fun `typealias with generic`() {
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
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").firstSignature()
                    .match(
                        "public final ", A("Integer"), A("someFun"), "(", Parameters(
                            Parameter(A("Map"), "<", A("String"), ", ", A("Integer"), "> xd"),
                        ), ")", ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @Test
    fun `const in top level`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |const val FIRST = "String"
        """.trimMargin(),
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                assertNull(writerPlugin.writer.contents["root/kotlinAsJavaPlugin/-test-kt/get-f-i-r-s-t.html"])
            }
        }
    }

    @Test
    fun `function in top level`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |fun sample(a: Int) = ""
        """.trimMargin(),
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-test-kt/sample.html").firstSignature()
                    .match(
                        "public final static ", A("String"), A("sample"), "(", Parameters(
                            Parameter(A("Integer"), "a"),
                        ), ")", ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @Test
    fun `should render primary kotlin constructor as a java constructor`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test(val xd: Int)
        """.trimMargin(),
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = root.children
                    .flatMap { it.children<ContentPage>() }
                    .map { it.content }.single().mainContents

                val text = content.single { it is ContentHeader }.children
                    .single() as ContentText

                assertEquals("Constructors", text.text)
            }
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-test/-test.html").firstSignature().match(
                    A("Test"), A("Test"), "(", Parameters(
                        Parameter(A("Integer"), "xd")
                    ), ")", ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    /**
     * Kotlin Int becomes java int. Java int cannot be annotated in source, but Kotlin Int can be.
     * This is paired with DefaultDescriptorToDocumentableTranslatorTest.`Java primitive annotations work`()
     *
     * This test currently does not do anything because Kotlin.Int currently becomes java.lang.Integer not primitive int
     */
    @Test
    fun `Java primitive annotations work`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |@MustBeDocumented
            |@Target(AnnotationTarget.TYPE)
            |annotation class Hello()
            |fun bar(): @Hello() Int
        """.trimMargin(),
            stdlibConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            documentablesTransformationStage = { module ->
                val type = module.packages.single()
                    .classlikes.first { it.name == "TestKt" }
                    .functions.single()
                    .type as GenericTypeConstructor
                assertEquals(
                    Annotations.Annotation(DRI("kotlinAsJavaPlugin", "Hello"), emptyMap()),
                    type.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                )
                // A bug; the GenericTypeConstructor cast should fail and this should be a PrimitiveJavaType
                assertEquals("java.lang/Integer///PointingToDeclaration/", type.dri.toString())
            }
        }
    }

    @OnlyJavaPsi
    @Test
    fun `Java function should keep its access modifier`() {
        val className = "Test"
        val accessModifier = "public"
        val methodName = "method"

        val testClassQuery = """
            |/src/main/kotlin/kotlinAsJavaPlugin/${className}.java
            |package kotlinAsJavaPlugin;
            |
            |public class $className {
            |   $accessModifier void ${methodName}() {
            |   
            |   }
            |}
            """.trimMargin()

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            testClassQuery,
            defaultConfiguration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                val methodDocumentation = "root/kotlinAsJavaPlugin/-${className.toLowerCase()}/${methodName}.html"

                writerPlugin.writer.renderedContent(methodDocumentation)
                    .firstSignature()
                    .match(
                        "$accessModifier void ", A(methodName), "()",
                        ignoreSpanWithTokenStyle = true
                    )
            }
        }
    }

    @Test
    fun `documentation on property getters and setters`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |/**
            | * @property propertyWithInheritedKDoc Property with kdoc from parent
            | */
            |class CrossLinksSource {
            |    var propertyWithInheritedKDoc: Int = 0
            |    
            |    /**
            |     * Property with own kdoc
            |     */
            |    var propertyWithOwnKDoc: Int = 0
            |}
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = (root.children.single().children
                    .first { it.name == "CrossLinksSource" } as ContentPage).content

                val functionRows = content.findTableWithKind(kind = ContentKind.Functions).children
                functionRows.assertCount(4)

                functionRows.first { it.dci.toString().contains("getPropertyWithInheritedKDoc") }
                    .assertGetter("getPropertyWithInheritedKDoc", "Property with kdoc from parent")

                functionRows.first { it.dci.toString().contains("setPropertyWithInheritedKDoc") }
                    .assertSetter("setPropertyWithInheritedKDoc", "Property with kdoc from parent")

                functionRows.first { it.dci.toString().contains("getPropertyWithOwnKDoc") }
                    .assertGetter("getPropertyWithOwnKDoc", "Property with own kdoc")

                functionRows.first { it.dci.toString().contains("setPropertyWithOwnKDoc") }
                    .assertSetter("setPropertyWithOwnKDoc", "Property with own kdoc")
            }
        }
    }

    @Test
    fun `documentation on properties with custom property accessors`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |/**
            | * @property propertyWithInheritedKDoc Property with kdoc from parent
            | */
            |class CrossLinksSource {
            |    var propertyWithInheritedKDoc: Int 
            |            get() = 0 
            |            set(value) {}
            |    
            |    /**
            |     * Property with own kdoc
            |     */
            |    var propertyWithOwnKDoc: Int 
            |            get() = 0 
            |            set(value) {}
            |}
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = (root.children.single().children
                    .first { it.name == "CrossLinksSource" } as ContentPage).content

                val functionRows = content.findTableWithKind(kind = ContentKind.Functions).children
                functionRows.assertCount(4)

                functionRows.first { it.dci.toString().contains("getPropertyWithInheritedKDoc") }
                    .assertGetter("getPropertyWithInheritedKDoc", "Property with kdoc from parent")

                functionRows.first { it.dci.toString().contains("setPropertyWithInheritedKDoc") }
                    .assertSetter("setPropertyWithInheritedKDoc", "Property with kdoc from parent")

                functionRows.first { it.dci.toString().contains("getPropertyWithOwnKDoc") }
                    .assertGetter("getPropertyWithOwnKDoc", "Property with own kdoc")

                functionRows.first { it.dci.toString().contains("setPropertyWithOwnKDoc") }
                    .assertSetter("setPropertyWithOwnKDoc", "Property with own kdoc")
            }
        }
    }

    @Test
    fun `documentation on extension property getter`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |/**
            | * Property with own kdoc
            | */
            |val String.extensionPropertyWithKDoc: Int get() = 0
        """.trimMargin(),
            defaultConfiguration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content =
                    (root.children.single().children.first { it.name == "TestKt" } as ContentPage).content

                val functionRows = content.findTableWithKind(kind = ContentKind.Functions).children
                functionRows.assertCount(1)
                functionRows.first().assertGetter(
                    "getExtensionPropertyWithKDoc",
                    "Property with own kdoc",
                )
            }
        }
    }

    private fun ContentNode.assertGetter(name: String, docs: String) = assertNode {
        link { +name }

        divergentGroup {
            divergentInstance {
                group3 {
                    +"public final "
                    group { link { +"Integer" } }
                    link { +name }
                    +"()"
                }
                group4 {
                    +docs
                }
            }
        }
    }

    private fun ContentNode.assertSetter(name: String, docs: String) = assertNode {
        link { +name }

        divergentGroup {
            divergentInstance {
                group3 {
                    +"public final "
                    group { link { +"Unit" } }
                    link { +name }
                    +"("
                    group2 {
                        groupedLink { +"Integer" }
                        // ignore the parameter name
                        skipAllNotMatching()
                    }
                    +")"
                }
                group4 {
                    +docs
                }
            }
        }
    }

    @Test
    fun `accessors should have source links`() {
        fun Element.getSourceLink() = select(".symbol .source-link")
            .select("a[href]")
            .attr("href")

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/kotlin",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/kotlin"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |class DefValue {
            |    val str: String = "defString"
            |}

        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/testpackage/-def-value/get-str.html")
                val sourceLink = page.getSourceLink()

                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/kotlin/basic/Deprecated.kt#L4",
                    sourceLink
                )
            }
        }
    }
}

private val ContentNode.mainContents: List<ContentNode>
    get() = (this as ContentGroup).children
        .filterIsInstance<ContentGroup>()
        .single { it.dci.kind == ContentKind.Main }.children[0].let { it.children[0] }.children
