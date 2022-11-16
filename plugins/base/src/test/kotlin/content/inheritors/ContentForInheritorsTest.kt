package content.inheritors

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import utils.classSignature
import utils.findTestType

class ContentForInheritorsTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `class with one inheritor has table in description`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class Parent
            |
            |class Foo : Parent()
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Parent")
                println(page.content)
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Parent"
                            )
                            header(4) { +"Inheritors" }
                            table {
                                group {
                                    link { +"Foo" }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `interface with few inheritors has table in description`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |interface Parent
            | 
            |class Foo : Parent()
            |class Bar : Parent()
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Parent")
                println(page.content)
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            group {
                                +"interface "
                                link {
                                    +"Parent"
                                }
                            }
                            header(4) { +"Inheritors" }
                            table {
                                group {
                                    link { +"Foo" }
                                }
                                group {
                                    link { +"Bar" }
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