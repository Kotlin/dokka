package content.typealiases

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.junit.jupiter.api.Test
import utils.assertNotNull


class TypealiasTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    @Test
    fun `typealias should have a dedicated page with full documentation`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | /**
            | * Brief text
            | *
            | * some text
            | *
            | * @see String
            | * @throws Unit
            | */
            | typealias A = String
            """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val content = (module.dfs { it.name == "A" } as ClasslikePageNode).content
                val platformHinted = content.dfs { it is PlatformHintedContent }
                platformHinted.assertNotNull("platformHinted").assertNode {
                    group {
                        group {
                            group {
                                +"typealias "
                                group { group { link { +"A" } } }
                                +" = "
                                group { link { +"String" } }
                            }
                        }

                        group {
                            group {
                                group {
                                    group { +"Brief text" }
                                    group { +"some text" }
                                }
                            }
                        }

                        header { +"See also" }
                        table {
                            group { link { +"String" } }
                        }

                        header { +"Throws" }
                        table {
                            group { group { link { +"Unit" } } }
                        }
                    }
                }
            }
        }
    }
}