package content.signatures

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.functionSignature

class ConstructorsSignaturesTest : AbstractCoreTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `class name without parenthesis`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class"
                                link { +"SomeClass" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class name with empty parenthesis`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass()
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class"
                                link { +"SomeClass" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class with a parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(a: String)
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class"
                                link { +"SomeClass" }
                                +"(a:"
                                group { link { +"String" } }
                                +")"
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class with a val parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(val a: String)
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class"
                                link { +"SomeClass" }
                                +"(a:" // TODO: Make sure if we still do not want to have "val" here
                                group { link { +"String" } }
                                +")"
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class with a parameterless secondary constructor`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(a: String) {
            |    constructor()
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class"
                                link { +"SomeClass" }
                                +"(a:"
                                group { link { +"String" } }
                                +")"
                            }
                        }
                    }
                    group {
                        header { +"Constructors" }
                        table {
                            group {
                                link { +"<init>" }
                                functionSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "<init>"
                                )
                            }
                        }
                        skipAllNotMatching()
                    }
                }
            }
        }
    }
}
