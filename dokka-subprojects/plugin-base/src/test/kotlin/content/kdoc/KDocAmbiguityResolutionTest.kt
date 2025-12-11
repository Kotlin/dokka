/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.kdoc

import matchers.content.assertNode
import matchers.content.check
import matchers.content.divergentGroup
import matchers.content.divergentInstance
import matchers.content.group
import matchers.content.group2
import matchers.content.group3
import matchers.content.group4
import matchers.content.groupedLink
import matchers.content.header
import matchers.content.link
import matchers.content.platformHinted
import matchers.content.skipAllNotMatching
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToCallableParameters
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentPage
import utils.OnlySymbols
import utils.findTestType
import kotlin.test.Test
import kotlin.test.assertEquals

@OnlySymbols("New KDoc resolution")
class KDocAmbiguityResolutionTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `#3451 ambiguous link to the class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |/**
            | * [A]
            | */
            |class A {
            |    /**
            |     * [A]
            |     */
            |    class A
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                module.findTestType("test", "A")
                    .content.assertNode {
                        group {
                            header(1) { +"A" }

                            platformHinted {
                                group {
                                    +"class "
                                    link { +"A" }
                                }
                                group3 {
                                    link {
                                        check {
                                            assertEquals(
                                                "test/A///PointingToDeclaration/",
                                                (this as ContentDRILink).address.toString()
                                            )
                                        }
                                        +"A"
                                    }
                                }
                            }
                        }
                        skipAllNotMatching()
                    }

                module.findTestType("test", "A")
                    .firstChildOfType<ContentPage> { it is ClasslikePageNode && it.name == "A" }
                    .content.assertNode {
                        group {
                            header(1) { +"A" }

                            platformHinted {
                                group {
                                    +"class "
                                    link { +"A" }
                                }
                                group3 {
                                    link {
                                        check {
                                            assertEquals(
                                                "test/A.A///PointingToDeclaration/",
                                                (this as ContentDRILink).address.toString()
                                            )
                                        }
                                        +"A"
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
    fun `#3451 ambiguous link to function and property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |fun x() {}
            |/**
            | * [x]
            | */
            |val x: Int = 0
            |
            |/**
            | * [x]
            | */
            fun x2() {}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val pageX = module.findTestType("test", "x")
                pageX.content.assertNode {
                    group {
                        header(1) { +"x" }
                    }

                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"fun "
                                link { +"x" }
                                +"()"
                            }
                        }

                        divergentInstance {
                            group2 {
                                +"val "
                                link { +"x" }
                                +": "
                                groupedLink { +"Int" }
                                +" = 0"
                            }
                            group4 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI("test", null, Callable("x", null, emptyList(), isProperty = true)),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"x"
                                }
                            }
                        }
                    }
                }

                val pageX2 = module.findTestType("test", "x2")
                pageX2.content.assertNode {
                    group {
                        header(1) { +"x2" }
                    }

                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"fun "
                                link { +"x2" }
                                +"()"
                            }
                            group4 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI(
                                                "test",
                                                null,
                                                Callable("x", null, emptyList(), isProperty = false)
                                            ),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"x"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `#3451 ambiguous link with factory functions`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |/**
            | * [A] leads to the class
            | */
            |class A(i: Int)
            |
            |/**
            | * [A] leads to the factory function
            | */
            |fun A(a: Int, b: Int): A = A(a + b)
            |
            |/**
            | * [A] leads to the class
            | */
            |fun other() {}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.findTestType("test", "A") {
                    it.dri.toString() == "[test/A///PointingToDeclaration/]"
                }
                classPage.content.assertNode {
                    group {
                        header(1) { +"A" }

                        platformHinted {
                            group {
                                +"class "
                                link { +"A" }
                                +"("
                                group2 {
                                    +"i: "
                                    groupedLink { +"Int" }
                                }
                                +")"
                            }

                            group3 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI("test", "A", null),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"A"
                                }
                                +" leads to the class"
                            }
                        }
                    }
                    skipAllNotMatching()
                }

                val funPage = module.findTestType("test", "A") {
                    it.dri.toString() == "[test//A/#kotlin.Int#kotlin.Int/PointingToDeclaration/]"
                }
                funPage.content.assertNode {
                    group {
                        header(1) { +"A" }
                    }

                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"fun "
                                link { +"A" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        groupedLink { +"Int" }
                                        +", "
                                    }
                                    group {
                                        +"b: "
                                        groupedLink { +"Int" }
                                    }
                                }
                                +"): "
                                groupedLink { +"A" }
                            }
                            group4 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI(
                                                "test", null,
                                                Callable(
                                                    "A",
                                                    null,
                                                    listOf(
                                                        TypeConstructor("kotlin.Int", emptyList()),
                                                        TypeConstructor("kotlin.Int", emptyList())
                                                    )
                                                )
                                            ),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"A"
                                }
                                +" leads to the factory function"
                            }
                        }
                    }
                }

                val otherPage = module.findTestType("test", "other") {
                    it.dri.toString() == "[test//other/#/PointingToDeclaration/]"
                }
                otherPage.content.assertNode {
                    group {
                        header(1) { +"other" }
                    }

                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"fun "
                                link { +"other" }
                                +"()"
                            }
                            group4 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI("test", "A", null),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"A"
                                }
                                +" leads to the class"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `#3451 ambiguous link to inner class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |public open class A {
            |    inner class B
            |}
            |public class B
            |/**
            | * [B] leads to the inner class
            | */
            |class C : A()
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.findTestType("test", "C") {
                    it.dri.toString() == "[test/C///PointingToDeclaration/]"
                }
                classPage.content.assertNode {
                    group {
                        header(1) { +"C" }

                        platformHinted {
                            group {
                                +"class "
                                link { +"C" }
                                +" : "
                                link { +"A" }
                            }

                            group3 {
                                link {
                                    check {
                                        assertEquals(
                                            DRI("test", "A.B", null),
                                            (this as ContentDRILink).address
                                        )
                                    }
                                    +"B"
                                }
                                +" leads to the inner class"
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `#3179 KDoc link to property and parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |/**
            | * Link to param [a]
            | *
            | * Link to property [A.a]
            | */
            |class A(val a: Int)
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.findTestType("test", "A") {
                    it.dri.toString() == "[test/A///PointingToDeclaration/]"
                }
                classPage.content.assertNode {
                    group {
                        header(1) { +"A" }

                        platformHinted {
                            group {
                                +"class "
                                link { +"A" }
                                +"("
                                group2 {
                                    +"val a: "
                                    groupedLink { +"Int" }
                                }
                                +")"
                            }

                            group3 {
                                group {
                                    +"Link to param "
                                    link {
                                        check {
                                            assertEquals(
                                                DRI(
                                                    "test", "A",
                                                    Callable(
                                                        "",
                                                        null,
                                                        listOf(
                                                            TypeConstructor("kotlin.Int", emptyList()),
                                                        )
                                                    ),
                                                    PointingToCallableParameters(0)
                                                ),
                                                (this as ContentDRILink).address
                                            )
                                        }
                                        +"a"
                                    }
                                }
                                group {
                                    +"Link to property "
                                    link {
                                        check {
                                            assertEquals(
                                                DRI(
                                                    "test", "A",
                                                    Callable("a", null, emptyList(), isProperty = true)
                                                ),
                                                (this as ContentDRILink).address
                                            )
                                        }
                                        +"A.a"
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
