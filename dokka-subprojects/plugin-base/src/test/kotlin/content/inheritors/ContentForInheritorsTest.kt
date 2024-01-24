/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.inheritors

import matchers.content.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.OnlyDescriptors
import utils.classSignature
import utils.findTestType
import kotlin.test.Test
import kotlin.test.assertEquals

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
        pluginsConfigurations.add(
            PluginConfigurationImpl(
                DokkaBase::class.qualifiedName!!,
                DokkaConfiguration.SerializationFormat.JSON,
                """{ "mergeImplicitExpectActualDeclarations": true }""",
            )
        )
    }


    //Case from skiko library
    private val mppTestConfigurationSharedAsPlatform = dokkaConfiguration {
        moduleName = "example"
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
            }
            val jvm = sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "android"
                displayName = "android"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(jvm.value.sourceSetID)
                sourceRoots = listOf("src/androidMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "awt"
                displayName = "awt"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(jvm.value.sourceSetID)
                sourceRoots = listOf("src/awtMain/kotlin/pageMerger/Test.kt")
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

    @OnlyDescriptors("Order of inheritors is different in K2")
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
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "linuxX64",
                                        this.sourceSets.first().name
                                    )
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
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
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
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "common",
                                        this.sourceSets.first().name
                                    )
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
    fun `inheritors from merged classes`() {
        testInline(
            """
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |open class Parent
                |class LChild : Parent()
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |open class Parent
                |class JChild : Parent()
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
                                    link { +"JChild" }
                                }
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "jvm",
                                        this.sourceSets.first().name
                                    )
                                }
                            }
                            group {
                                +"open class "
                                link {
                                    +"Parent"
                                }
                            }
                            header(4) { +"Inheritors" }
                            table {
                                group {
                                    link { +"LChild" }
                                }
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "linuxX64",
                                        this.sourceSets.first().name
                                    )
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
    fun `merged inheritors from merged classes`() {
        testInline(
            """
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |open class Parent
                |class Child : Parent()
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |open class Parent
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
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "jvm",
                                        this.sourceSets.first().name
                                    )
                                }
                            }
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
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "linuxX64",
                                        this.sourceSets.first().name
                                    )
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
    fun `parent in shared source set that analyse as platform`() {
        testInline(
            """
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |interface Parent
                |
                |/src/androidMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class Child : Parent
                |
                |/src/awtMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |class AwtChild : Parent
                |class Child : Parent
                |
            """.trimMargin(),
            mppTestConfigurationSharedAsPlatform
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "Parent")
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
                                    link { +"Child" }
                                }
                                group {
                                    link { +"AwtChild" }
                                }
                                check {
                                    assertEquals(1, sourceSets.size)
                                    assertEquals(
                                        "jvm",
                                        this.sourceSets.first().name
                                    )
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
