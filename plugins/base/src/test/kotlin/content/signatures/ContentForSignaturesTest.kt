package content.signatures

import matchers.content.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.bareSignature
import utils.propertySignature
import utils.typealiasSignature

class ContentForSignaturesTest : AbstractCoreTest() {

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                includeNonPublic = true
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "private",
                                    "",
                                    emptySet(),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "open",
                                    emptySet(),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `function without parameters`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(): String {
            |    return "Hello"
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "function",
                                    returnType = "String",
                                )
                            }
                        }
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    setOf("suspend"),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "protected",
                                    "open",
                                    setOf("suspend"),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "protected",
                                    "open",
                                    setOf("inline", "suspend"),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
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
                    propertySignature(emptyMap(), "", "", emptySet(), "val", "property", "Int")
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
                    propertySignature(emptyMap(), "", "", setOf("const"), "val", "property", "Int")
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
                    propertySignature(emptyMap(), "protected", "", emptySet(), "val", "property", "Int")
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
                    propertySignature(emptyMap(), "protected", "", setOf("lateinit"), "var", "property", "Int")
                }
            }
        }
    }

    @Test
    fun `typealias to String`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |typealias Alias = String
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    typealiasSignature("Alias", "String")
                }
            }
        }
    }

    @Test
    fun `typealias to Int`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |typealias Alias = Int
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    typealiasSignature("Alias", "Int")
                }
            }
        }
    }

    @Test
    fun `typealias to type in same package`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |typealias Alias = X
            |class X
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    typealiasSignature("Alias", "X")
                }
            }
        }
    }

    @Test
    fun `typealias to type in different package`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |import other.X
            |typealias Alias = X
            |
            |/src/main/kotlin/test/source2.kt
            |package other
            |class X
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    typealiasSignature("Alias", "other.X")
                }
            }
        }
    }
}
