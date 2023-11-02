/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.AllTypesPageNode
import org.jetbrains.dokka.pages.RootPageNode
import utils.withAllTypesPage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AllTypesPageTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    private fun RootPageNode.allTypesPageNode(): AllTypesPageNode? =
        children.singleOrNull { it is AllTypesPageNode } as? AllTypesPageNode

    @Test
    fun `all types page generated when there are types`() = withAllTypesPage {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |/**
            | * Hello World!
            | * 
            | * Some other comment which should not be on All Types page
            | */
            |public class Test
            |
            |/**
            | * Hello type
            | */
            |public typealias Alias = Int
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                assertNotNull(rootPage.allTypesPageNode()).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"All Types" }
                    table {
                        group {
                            link { +"sample.Alias" }
                            group {
                                group {
                                    +"Hello type"
                                }
                            }
                        }
                        group {
                            link { +"sample.Test" }
                            group {
                                group {
                                    +"Hello World!"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `all types isn't generated when not enabled by property`() {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |/**
            | * Hello World!
            | * 
            | * Some other comment which should not be on All Types page
            | */
            |public class Test
            |
            |/**
            | * Hello type
            | */
            |public typealias Alias = Int
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                assertNull(rootPage.allTypesPageNode())
            }
        }
    }

    @Test
    fun `all types page isn't generated when there are NO types`() = withAllTypesPage {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |/**
            | * Hello World!
            | */
            |public fun test() {}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                assertNull(rootPage.allTypesPageNode())
            }
        }
    }

    @Test
    fun `all types sorting depends only on simple name`() = withAllTypesPage {
        testInline(
            """
            |/src/A.kt
            |package bbb
            |public class A
            |/src/B.kt
            |package xxx
            |public class B
            |/src/C.kt
            |package aaa
            |public class C
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                assertNotNull(rootPage.allTypesPageNode()).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"All Types" }
                    table {
                        group {
                            link { +"bbb.A" }
                        }
                        group {
                            link { +"xxx.B" }
                        }
                        group {
                            link { +"aaa.C" }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `all types description should be taken from most relevant sourceSet`() = withAllTypesPage {
        testInline(
            """
            |/src/common/test.kt
            |package test
            |/**
            | * Common
            | */
            |expect class FromCommon
            |expect class FromJvm
            |expect class FromNative
            |/src/jvm/test.kt
            |package test
            |/**
            | * JVM
            | */
            |actual class FromCommon
            |/**
            | * JVM
            | */
            |actual class FromJvm
            |actual class FromNative
            |/src/native/test.kt
            |package test
            |/**
            | * Native
            | */
            |actual class FromCommon
            |actual class FromJvm
            |/**
            | * Native
            | */
            |actual class FromNative
            """.trimIndent(),
            dokkaConfiguration {
                sourceSets {
                    val commonId = sourceSet {
                        sourceRoots = listOf("src/common/")
                        analysisPlatform = "common"
                        name = "common"
                    }.value.sourceSetID
                    sourceSet {
                        sourceRoots = listOf("src/jvm/")
                        analysisPlatform = "jvm"
                        name = "jvm"
                        dependentSourceSets = setOf(commonId)
                    }
                    sourceSet {
                        sourceRoots = listOf("src/native/")
                        analysisPlatform = "native"
                        name = "native"
                        dependentSourceSets = setOf(commonId)
                    }
                }
            }
        ) {
            pagesTransformationStage = { rootPage ->
                assertNotNull(rootPage.allTypesPageNode()).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"All Types" }
                    table {
                        group {
                            link { +"test.FromCommon" }
                            group { group { +"Common" } }
                        }
                        group {
                            link { +"test.FromJvm" }
                            group { group { +"JVM" } }
                        }
                        group {
                            link { +"test.FromNative" }
                            group { group { +"Native" } }
                        }
                    }
                }
            }
        }
    }
}
