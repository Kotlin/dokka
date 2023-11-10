/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.exceptions

import matchers.content.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DisplaySourceSet
import utils.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentForExceptions : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
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
                classpath = listOfNotNull(commonStdlibPath)
            }
            sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                classpath = listOfNotNull(jvmStdlibPath)
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

    @OnlyDescriptors("Fixed in 1.9.20 (IMPORT STAR)")
    @Test
    fun `function with navigatable thrown exception`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |/**
            |* @throws Exception
            |*/
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
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
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            link { +"Exception" }
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
    fun `function with non-navigatable thrown exception`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |/**
            |* @throws UnavailableException
            |*/
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
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
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            +"UnavailableException"
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
    fun `multiplatofrm class with throws`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws CommonException
                |*/
                |expect open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws JvmException
                |*/
                |actual open class Parent
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws LinuxException
                |*/
                |actual open class Parent
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
                            header(4) { +"Throws" }
                            table {
                                group {
                                    group {
                                        +"CommonException"
                                    }
                                    check {
                                        assertEquals(1, sourceSets.size)
                                        assertEquals(
                                            "common",
                                            this.sourceSets.first().name
                                        )
                                    }
                                }
                                group {
                                    group {
                                        +"JvmException"
                                    }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }
                                group {
                                    group {
                                        +"LinuxException"
                                    }
                                    check {
                                        sourceSets.assertSourceSet("linuxX64")
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

    @Test
    fun `multiplatofrm class with throws in few platforms`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws CommonException
                |*/
                |expect open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws JvmException
                |*/
                |actual open class Parent
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual open class Parent
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
                            header(4) { +"Throws" }
                            table {
                                group {
                                    group {
                                        +"CommonException"
                                    }
                                    check {
                                        sourceSets.assertSourceSet("common")
                                    }
                                }
                                group {
                                    group {
                                        +"JvmException"
                                    }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }
                                check {
                                    assertEquals(2, sourceSets.size)
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
    fun `throws in merged functions`() {
        testInline(
            """
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws LinuxException
                |*/
                |fun function() {
                |    println()
                |}
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @throws JvmException
                |*/
                |fun function() {
                |    println()
                |}
                |
            """.trimMargin(),
            mppTestConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "function")
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
                                    null,
                                )
                            }
                            after {
                                header(4) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            +"JvmException"
                                        }
                                    }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }
                            }
                            check {
                                sourceSets.assertSourceSet("jvm")
                            }
                        }
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                )
                            }
                            after {
                                header(4) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            +"LinuxException"
                                        }
                                    }
                                }
                            }
                            check {
                                sourceSets.assertSourceSet("linuxX64")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Set<DisplaySourceSet>.assertSourceSet(expectedName: String) {
    assertEquals(1, this.size)
    assertEquals(expectedName, this.first().name)
}
