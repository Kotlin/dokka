package content.signatures

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.functionSignature

class ConstructorsSignaturesTest : BaseAbstractTest() {
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
                                +"class "
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
                                +"class "
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
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
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
            |class SomeClass(val a: String, var i: Int)
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
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"val a: "
                                        group { link { +"String" } }
                                        +", "
                                    }
                                    group {
                                        +"var i: "
                                        group { link { +"Int" } }
                                    }
                                }
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
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    group {
                        header { +"Constructors" }
                        table {
                            group {
                                link { +"SomeClass" }
                                functionSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "SomeClass"
                                )
                            }
                            group {
                                link { +"SomeClass" }
                                functionSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "SomeClass",
                                    params = listOf("a" to ParamAttributes(emptyMap(), emptySet(), "String")).toTypedArray()
                                )
                            }
                        }
                        skipAllNotMatching()
                    }
                }
            }
        }
    }

    @Test
    fun `class with explicitly documented constructor`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            | /**
            |  * some comment
            |  * @constructor ctor comment
            | **/
            |class SomeClass(a: String)
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
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            skipAllNotMatching()
                        }
                    }
                    group {
                        header { +"Constructors" }
                        table {
                            group {
                                link { +"SomeClass" }
                                platformHinted {
                                    group {
                                        +"fun "
                                        link { +"SomeClass" }
                                        +"("
                                        group {
                                            group {
                                                +"a: "
                                                group {
                                                    link { +"String" }
                                                }
                                            }
                                        }
                                        +")"
                                    }
                                    group {
                                        group {
                                            group { +"ctor comment" }
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
    }
}
