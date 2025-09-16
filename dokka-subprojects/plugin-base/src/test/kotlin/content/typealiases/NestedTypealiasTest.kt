/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.typealiases

import matchers.content.assertNode
import matchers.content.divergentGroup
import matchers.content.divergentInstance
import matchers.content.group
import matchers.content.header
import matchers.content.link
import matchers.content.platformHinted
import matchers.content.table
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PlatformHintedContent
import utils.assertNotNull
import utils.findTestType
import kotlin.test.Test

class NestedTypealiasTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!, jvmStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }


    @Test
    fun `nested typealias in class`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |class Foo {
        |    /**
        |     * Brief text
        |     * 
        |     * some text
        |     *
        |     * @see String
        |     * @throws Unit
        |     */
        |    typealias A = String
        |}
        """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("example", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"Foo" }
                            }
                        }
                    }

                    group {
                        group {
                            group {
                                header(2) { +"Constructors" }
                                table {
                                    group {
                                        link { +"Foo" }
                                        platformHinted {
                                            group {
                                                +"constructor()"
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        group {
                            group {
                                header(2) { +"Types" }
                                table {
                                    group {
                                        link { +"A" }

                                        divergentGroup {
                                            divergentInstance {
                                                group {
                                                    group {
                                                        group {
                                                            group {
                                                                +"typealias "
                                                                group { group { link { +"Foo.A" } } }
                                                                +" = "
                                                                group { link { +"String" } }
                                                            }
                                                        }
                                                    }
                                                }

                                                group {
                                                    group {
                                                        group {
                                                            group { +"Brief text" }
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
                }
            }
        }
    }

    @Test
    fun `nested typealias dedicated page`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |class Foo {
        |    /**
        |     * Brief text
        |     * 
        |     * some text
        |     *
        |     * @see String
        |     * @throws Unit
        |     */
        |    typealias A = String
        |}
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
                                group { group { link { +"Foo.A" } } }
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