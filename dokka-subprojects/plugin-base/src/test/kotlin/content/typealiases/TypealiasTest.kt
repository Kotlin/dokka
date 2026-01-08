/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.typealiases

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.PlatformHintedContent
import utils.assertNotNull
import kotlin.test.Test


class TypealiasTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!, jvmStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    private val multiplatformConfiguration = dokkaConfiguration {
        sourceSets {
            val commonId = sourceSet {
                sourceRoots = listOf("src/common/")
                analysisPlatform = "common"
                name = "common"
                displayName = "common"
            }.value.sourceSetID
            sourceSet {
                sourceRoots = listOf("src/jvm/")
                analysisPlatform = "jvm"
                name = "jvm"
                displayName = "jvm"
                dependentSourceSets = setOf(commonId)
            }
            sourceSet {
                sourceRoots = listOf("src/native/")
                analysisPlatform = "native"
                name = "native"
                displayName = "native"
                dependentSourceSets = setOf(commonId)
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
            | * @throws Exception
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
                                group { link { +"A" } }
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
                            group { group { link { +"Exception" } } }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `typealias in a multiplatform project`() {
        testInline(
            """
            |/src/common/kotlin/test/test.kt
            |package example
            |expect class A
            |/src/jvm/kotlin/test/test.kt
            |package example
            |actual typealias A = String
            |/src/native/kotlin/test/test.kt
            |package example
            |actual class A
            """,
            multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.dfs { it.name == "A" } as ClasslikePageNode
                page.content.assertNode {
                    group {
                        header(1) { +"A" }
                        platformHinted {
                            group {
                                +"expect class "
                                link { +"A" }
                            }
                            group {
                                +"actual class "
                                link { +"A" }
                            }
                            group2 {
                                +"actual typealias "
                                groupedLink { +"A" }
                                +" = "
                                groupedLink { +"String" }
                            }
                        }
                    }

                    group {
                        table2("Constructors") {
                            group {
                                link { +"A" }
                                platformHinted {
                                    group {
                                        +"constructor()"
                                    }
                                }
                            }
                        }

                        group { }
                    }
                }
            }
        }
    }

    @Test
    fun `non-nullable typealias to nullable type`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |typealias A = String?
            |
            |val a: A
            """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val content = (module.dfs { it.name == "a" } as MemberPageNode).content
                content.assertNode {
                    group {
                        header(1) { +"a"  }
                    }
                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"val "
                                link { +"a" }
                                +": "
                                groupedLink { +"A" }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nullable typealias to non-nullable type`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |typealias A = String
            |
            |val a: A?
            """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val content = (module.dfs { it.name == "a" } as MemberPageNode).content
                content.assertNode {
                    group {
                        header(1) { +"a"  }
                    }
                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"val "
                                link { +"a" }
                                +": "
                                group {
                                    groupedLink { +"A" }
                                    +"?"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nullable typealias to nullable type`() {
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |typealias A = String?
            |
            |val a: A?
            """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val content = (module.dfs { it.name == "a" } as MemberPageNode).content
                content.assertNode {
                    group {
                        header(1) { +"a"  }
                    }
                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"val "
                                link { +"a" }
                                +": "
                                group {
                                    groupedLink { +"A" }
                                    +"?"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
