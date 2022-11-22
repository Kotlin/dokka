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

    private val mppTestConfiguration = dokkaConfiguration {
        moduleName = "example"
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "linuxX64"
                displayName = "linuxX64"
                analysisPlatform = "native"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
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

    @Test
    fun `inherit from one of multiplatoforms actuals`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual open class Parent
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual open class Parent
                |class Child: Parent()
                |
            """.trimMargin(),
            mppTestConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "Parent")
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            group {
                                +"expect open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
                            header(4) { +"Inheritors" }
                            table {
                                group {
                                    link { +"Child" }
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
    fun `inherit from class in common code`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class Child : Parent()
                |
            """.trimMargin(),
            mppTestConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "Parent")
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            group {
                                +"open class "
                                link {
                                    +"Parent"
                                }
                            }
                            header(4) { +"Inheritors" }
                            table {
                                group {
                                    link { +"Child" }
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