package content.functions

import org.junit.Assert.assertEquals
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.junit.jupiter.api.Test
import kotlin.test.assertNull


class ContentForBriefTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    private val codeWithSecondaryAndPrimaryConstructorsDocumented =
        """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |/**
            | * Dummy text.
            | *
            | * @constructor constructor docs
            | * @param exampleParameter dummy parameter.
            | */
            |class Example(val exampleParameter: Int) {
            | 
            |    /**
            |     * secondary constructor
            |     * @param param1 param1 docs
            |     */
            |    constructor(param1: String) : this(1)
            |}
        """.trimIndent()

    private val codeWithDocumentedParameter =
        """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |/**
            | * Dummy text.
            | *
            | * @param exampleParameter dummy parameter.
            | */
            |class Example(val exampleParameter: Int) {
            |}
        """.trimIndent()

    @Test
    fun `primary constructor should not inherit docs from its parameter`() {
        testInline(codeWithSecondaryAndPrimaryConstructorsDocumented, testConfiguration) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.dfs { it.name == "Example" && (it as ContentPage).documentable is DClass } as ContentPage
                val constructorsTable =
                    classPage.content.dfs { it is ContentTable && it.dci.kind == ContentKind.Constructors } as ContentTable

                assertEquals(2, constructorsTable.children.size)
                val primary = constructorsTable.children.first {
                    it.dci.dri.first().callable?.params?.first() == TypeConstructor(
                        "kotlin.Int",
                        emptyList()
                    )
                }
                val primaryConstructorDocs =
                    primary.dfs { it is ContentText && it.dci.kind == ContentKind.Comment } as ContentText

                assertEquals("constructor docs", primaryConstructorDocs.text)
            }
        }
    }

    @Test
    fun `secondary constructor should not inherit docs from its parameter`() {
        testInline(codeWithSecondaryAndPrimaryConstructorsDocumented, testConfiguration) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.dfs { it.name == "Example" && (it as ContentPage).documentable is DClass } as ContentPage
                val constructorsTable =
                    classPage.content.dfs { it is ContentTable && it.dci.kind == ContentKind.Constructors } as ContentTable

                assertEquals(2, constructorsTable.children.size)
                val primary = constructorsTable.children.first {
                    it.dci.dri.first().callable?.params?.first() == TypeConstructor(
                        "kotlin.String",
                        emptyList()
                    )
                }
                val primaryConstructorDocs =
                    primary.dfs { it is ContentText && it.dci.kind == ContentKind.Comment } as ContentText

                assertEquals("secondary constructor", primaryConstructorDocs.text)
            }
        }
    }

    @Test
    fun `primary constructor should not inherit docs from its parameter when no specific docs are provided`() {
        testInline(codeWithDocumentedParameter, testConfiguration) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.dfs { it.name == "Example" && (it as ContentPage).documentable is DClass } as ContentPage
                val constructorsTable =
                    classPage.content.dfs { it is ContentTable && it.dci.kind == ContentKind.Constructors } as ContentTable

                assertEquals(1, constructorsTable.children.size)
                val primary = constructorsTable.children.first()
                val primaryConstructorDocs = primary.dfs { it is ContentText && it.dci.kind == ContentKind.Comment }

                assertNull(primaryConstructorDocs, "Expected no primary constructor docs to be present")
            }
        }
    }

    @Test
    fun `brief for functions should work with html`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class Example(val exampleParameter: Int) {
            |   /**
            |    * This is an example <!-- not visible --> of html
            |    *
            |    * This is definitely not a brief
            |    */
            |   fun test(): String = "TODO"
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "This is an example <!-- not visible --> of html",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    @Test
    fun `brief for functions should work with ie`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class Example(val exampleParameter: Int) {
            |   /**
            |    * The user token, i.e. "Bearer xyz". Throw an exception if not available.
            |    *
            |    * This is definitely not a brief
            |    */
            |   fun test(): String = "TODO"
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "The user token, i.e. \"Bearer xyz\". Throw an exception if not available.",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    @Test
    fun `brief for functions should work with eg`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class Example(val exampleParameter: Int) {
            |   /**
            |    * The user token, e.g. "Bearer xyz". Throw an exception if not available.
            |    *
            |    * This is definitely not a brief
            |    */
            |   fun test(): String = "TODO"
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "The user token, e.g. \"Bearer xyz\". Throw an exception if not available.",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    @Test
    fun `brief for functions should be first sentence for Java`() {
        testInline(
            """
            |/src/main/java/test/Example.java
            |package test;
            |
            |public class Example {
            |   /**
            |    * The user token, or not. This is definitely not a brief in java
            |    */
            |   public static String test() {
            |       return "TODO";
            |   }
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "The user token, or not.",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    @Test
    fun `brief for functions should work with ie for Java`() {
        testInline(
            """
            |/src/main/java/test/Example.java
            |package test;
            |
            |public class Example {
            |   /**
            |    * The user token, e.g.&nbsp;"Bearer xyz". This is definitely not a brief in java
            |    */
            |   public static String test() {
            |       return "TODO";
            |   }
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "The user token, e.g. \"Bearer xyz\".",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    //Source: https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html#exampleresult
    @Test
    fun `brief for functions should work with html comment for Java`() {
        testInline(
            """
            |/src/main/java/test/Example.java
            |package test;
            |
            |public class Example {
            |   /**
            |    * This is a simulation of Prof.<!-- --> Knuth's MIX computer. This is definitely not a brief in java
            |    */
            |   public static String test() {
            |       return "TODO";
            |   }
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "This is a simulation of Prof.<!-- --> Knuth's MIX computer.",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    @Test
    fun `brief for functions should work with html comment at the end for Java`() {
        testInline(
            """
            |/src/main/java/test/Example.java
            |package test;
            |
            |public class Example {
            |   /**
            |    * This is a simulation of Prof.<!-- --> Knuth's MIX computer. This is definitely not a brief in java <!-- -->
            |    */
            |   public static String test() {
            |       return "TODO";
            |   }
            |}
        """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionBriefDocs = module.singleFunctionDescription("Example")

                assertEquals(
                    "This is a simulation of Prof.<!-- --> Knuth's MIX computer.",
                    functionBriefDocs.children.joinToString("") { (it as ContentText).text })
            }
        }
    }

    private fun RootPageNode.singleFunctionDescription(className: String): ContentGroup {
        val classPage = dfs { it.name == className && (it as ContentPage).documentable is DClass } as ContentPage
        val functionsTable =
            classPage.content.dfs { it is ContentTable && it.dci.kind == ContentKind.Functions } as ContentTable

        assertEquals(1, functionsTable.children.size)
        val function = functionsTable.children.first()
        return function.dfs { it is ContentGroup && it.dci.kind == ContentKind.Comment && it.children.all { it is ContentText } } as ContentGroup
    }
}