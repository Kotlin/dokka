/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content

import matchers.content.*
import org.jetbrains.dokka.base.pages.AllTypesPageNode
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ModulePageNode
import utils.withAllTypesPage
import kotlin.test.Test
import kotlin.test.assertEquals

class ModulePageTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `show packages content`() = withAllTypesPage {
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
                (rootPage as ModulePageNode).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"Packages" }
                    table {
                        group {
                            link { +"aaa" }
                        }
                        group {
                            link { +"bbb" }
                        }
                        group {
                            link { +"xxx" }
                        }
                    }
                    header { +"Index" }
                    link { +"All Types" }
                }
            }
        }
    }

    @Test
    fun `show link to all types page when there are types`() = withAllTypesPage {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |public class Test
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                (rootPage as ModulePageNode).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"Packages" }
                    table { skipAllNotMatching() }
                    header { +"Index" }
                    link {
                        +"All Types"
                        check {
                            assertEquals(AllTypesPageNode.DRI, (this as ContentDRILink).address)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `no link to all types page when there are no types`() {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |public fun test() {}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { rootPage ->
                (rootPage as ModulePageNode).content.assertNode {
                    group {
                        header { +"root" } // module name
                    }
                    header { +"Packages" }
                    table { skipAllNotMatching() }
                }
            }
        }
    }
}
