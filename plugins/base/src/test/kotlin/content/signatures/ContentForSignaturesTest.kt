package content.signatures

import matchers.content.*
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.Test
import utils.functionSignature
import utils.propertySignature

class ContentForSignaturesTest : AbstractCoreTest() {

    private val testConfiguration = dokkaConfiguration {
        passes {
            pass {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                targets = listOf("jvm")
            }
        }
    }

    @Test
    fun `function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "", "", emptySet(), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `private function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |private fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "private", "", emptySet(), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `open function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |open fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "", "open", emptySet(), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `suspend function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |suspend fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "", "", setOf("suspend"), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `protected open suspend function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |protected open suspend fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "protected", "open", setOf("suspend"), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `protected open suspend inline function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |protected open suspend inline fun function(abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(emptySet(), "protected", "open", setOf("inline", "suspend"), "function","String", "abc" to mapOf("Type" to setOf("String")))
                    }
                }
            }
        }
    }

    @Test
    fun `property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |val property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(emptySet(), "", "", emptySet(), "val", "property","Int")
                }
            }
        }
    }

    @Test
    fun `const property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |const val property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(emptySet(), "", "", setOf("const"), "val", "property","Int")
                }
            }
        }
    }

    @Test
    fun `protected property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |protected val property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(emptySet(), "protected", "", emptySet(), "val", "property","Int")
                }
            }
        }
    }

    @Test
    fun `protected lateinit property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |protected lateinit var property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(emptySet(), "protected", "", setOf("lateinit"), "var", "property","Int")
                }
            }
        }
    }
}