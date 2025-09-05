/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.signatures

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.BasicTabbedContentType
import org.jetbrains.dokka.pages.ContentPage
import kotlin.test.Test
import utils.OnlyDescriptors
import kotlin.test.assertEquals

class ConstructorsSignaturesTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
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
        }
    }

    @Test
    fun `class name without parenthesis`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class name with empty parenthesis`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass()
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class with a parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(a: String)
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `class with a val parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(val a: String, var i: Int)
            |
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"val a: "
                                        group { link { +"String" } }
                                        +", "
                                    }
                                    group {
                                        +"var i: "
                                        group { link { +"Int" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @OnlyDescriptors("Order of constructors is different in K2")
    @Test
    fun `class with a parameterless secondary constructor`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |class SomeClass(a: String) {
            |    constructor()
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    tabbedGroup {
                        group {
                            tab(BasicTabbedContentType.CONSTRUCTOR) {
                                header { +"Constructors" }
                                table {
                                    group {
                                        link { +"SomeClass" }
                                        platformHinted {
                                            group {
                                                +"constructor"
                                                +"("
                                                +")"
                                            }
                                            group {
                                                +"constructor"
                                                +"("
                                                group {
                                                    group {
                                                        +"a: "
                                                        group { link { +"String" } }
                                                    }
                                                }
                                                +")"
                                            }
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


    @OnlyDescriptors("Order of constructors is different in K2")
    @Test
    fun `class with a few documented constructors`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            | /**
            |  * some comment
            |  * @constructor ctor comment
            | **/
            |class SomeClass(a: String){
            |    /**
            |     * ctor one
            |    **/
            |    constructor(): this("")
            |
            |    /**
            |     * ctor two
            |    **/
            |    constructor(b: Int): this("")
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            skipAllNotMatching()
                        }
                    }
                    tabbedGroup {
                        group {
                            tab(BasicTabbedContentType.CONSTRUCTOR) {
                                header { +"Constructors" }
                                table {
                                    group {
                                        link { +"SomeClass" }
                                        platformHinted {
                                            group {
                                                +"constructor"
                                                +"("
                                                +")"
                                            }
                                            group {
                                                group {
                                                    group { +"ctor one" }
                                                }
                                            }
                                            group {
                                                +"constructor"
                                                +"("
                                                group {
                                                    group {
                                                        +"b: "
                                                        group {
                                                            link { +"Int" }
                                                        }
                                                    }
                                                }
                                                +")"
                                            }
                                            group {
                                                group {
                                                    group { +"ctor two" }
                                                }
                                            }
                                            group {
                                                +"constructor"
                                                +"("
                                                group {
                                                    group {
                                                        +"a: "
                                                        group {
                                                            link { +"String" }
                                                        }
                                                    }
                                                }
                                                +")"
                                            }
                                            group {
                                                group {
                                                    group { +"ctor comment" }
                                                }
                                            }
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

    @Test
    fun `class with explicitly documented constructor`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            | /**
            |  * some comment
            |  * @constructor ctor comment
            | **/
            |class SomeClass(a: String)
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "SomeClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"SomeClass" }
                        platformHinted {
                            group {
                                +"class "
                                link { +"SomeClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            skipAllNotMatching()
                        }
                    }
                    tabbedGroup {
                        group {
                            tab(BasicTabbedContentType.CONSTRUCTOR) {
                                header { +"Constructors" }
                                table {
                                    group {
                                        link { +"SomeClass" }
                                        platformHinted {
                                            group {
                                                +"constructor"
                                                +"("
                                                group {
                                                    group {
                                                        +"a: "
                                                        group {
                                                            link { +"String" }
                                                        }
                                                    }
                                                }
                                                +")"
                                            }
                                            group {
                                                group {
                                                    group { +"ctor comment" }
                                                }
                                            }
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

    @Test
    fun `should render primary constructor, but not constructors block for annotation class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |annotation class MyAnnotation(val param: String) {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "MyAnnotation" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"MyAnnotation" }
                        platformHinted {
                            group {
                                +"annotation class "
                                link { +"MyAnnotation" }
                                +"("
                                group {
                                    group {
                                        +"val param: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header { +"Properties" }
                                table {
                                    skipAllNotMatching()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `expect class without constructor should have only primary constructor`() {
        testInline(
            """
            |/src/common/test.kt
            |expect class ExpectActualClass
            |/src/jvm/test.kt
            |actual class ExpectActualClass
        """.trimIndent(), multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page =
                    module.children.single { it.name == "[root]" }
                        .children.single { it.name == "ExpectActualClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"ExpectActualClass" }
                        platformHinted {
                            check {
                                assertEquals(setOf("common", "jvm"), sourceSets.map(DisplaySourceSet::name).toSet())
                            }
                            group {
                                check {
                                    assertEquals("common", this.sourceSets.single().name)
                                }
                                +"expect class "
                                link { +"ExpectActualClass" }
                            }
                            group {
                                check {
                                    assertEquals("jvm", this.sourceSets.single().name)
                                }
                                +"actual class "
                                link { +"ExpectActualClass" }
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header(2) { +"Constructors" }
                                table {
                                    group {
                                        link { +"ExpectActualClass" }
                                        platformHinted {
                                            check {
                                                // constructor should exist only for jvm source set
                                                assertEquals(
                                                    setOf("jvm"),
                                                    sourceSets.map(DisplaySourceSet::name).toSet()
                                                )
                                            }
                                            group {
                                                check { assertEquals("jvm", this.sourceSets.single().name) }
                                                // no actual modifier
                                                +"constructor()"
                                            }
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

    @Test
    fun `expect class with empty constructor should have only both expect and actual constructor`() {
        testInline(
            """
            |/src/common/test.kt
            |expect class ExpectActualClass()
            |/src/jvm/test.kt
            |actual class ExpectActualClass actual constructor()
        """.trimIndent(), multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page =
                    module.children.single { it.name == "[root]" }
                        .children.single { it.name == "ExpectActualClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"ExpectActualClass" }
                        platformHinted {
                            check {
                                assertEquals(
                                    setOf("common", "jvm"),
                                    sourceSets.map(DisplaySourceSet::name).toSet()
                                )
                            }
                            group {
                                check {
                                    assertEquals("common", this.sourceSets.single().name)
                                }
                                +"expect class "
                                link { +"ExpectActualClass" }
                            }
                            group {
                                check {
                                    assertEquals("jvm", this.sourceSets.single().name)
                                }
                                +"actual class "
                                link { +"ExpectActualClass" }
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header(2) { +"Constructors" }
                                table {
                                    group {
                                        link { +"ExpectActualClass" }
                                        platformHinted {
                                            check {
                                                assertEquals(
                                                    setOf("common", "jvm"),
                                                    sourceSets.map(DisplaySourceSet::name).toSet()
                                                )
                                            }
                                            group {
                                                check { assertEquals("common", this.sourceSets.single().name) }
                                                +"expect constructor()"
                                            }
                                            group {
                                                check { assertEquals("jvm", this.sourceSets.single().name) }
                                                +"actual constructor()"
                                            }
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

    @Test
    fun `expect class with constructor with parameter`() {
        testInline(
            """
            |/src/common/test.kt
            |expect class ExpectActualClass(a: String)
            |/src/jvm/test.kt
            |actual class ExpectActualClass actual constructor(a: String)
        """.trimIndent(), multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page =
                    module.children.single { it.name == "[root]" }
                        .children.single { it.name == "ExpectActualClass" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"ExpectActualClass" }
                        platformHinted {
                            check {
                                assertEquals(setOf("common", "jvm"), sourceSets.map(DisplaySourceSet::name).toSet())
                            }
                            group {
                                check {
                                    assertEquals("common", this.sourceSets.single().name)
                                }
                                +"expect class "
                                link { +"ExpectActualClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            group {
                                check {
                                    assertEquals("jvm", this.sourceSets.single().name)
                                }
                                +"actual class "
                                link { +"ExpectActualClass" }
                                +"("
                                group {
                                    group {
                                        +"a: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header(2) { +"Constructors" }
                                table {
                                    group {
                                        link { +"ExpectActualClass" }
                                        platformHinted {
                                            check {
                                                println(this)
                                            }
                                            group {
                                                check { assertEquals("common", this.sourceSets.single().name) }
                                                +"expect constructor("
                                                group {
                                                    group {
                                                        +"a: "
                                                        group { link { +"String" } }
                                                    }
                                                }
                                                +")"
                                            }
                                            group {
                                                check { assertEquals("jvm", this.sourceSets.single().name) }
                                                +"actual constructor("
                                                group {
                                                    group {
                                                        +"a: "
                                                        group { link { +"String" } }
                                                    }
                                                }
                                                +")"
                                            }
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

    @Test
    fun `annotation class with explicit actual constructor`() {
        testInline(
            """
            |/src/common/test.kt
            |expect annotation class A(val name: String)
            |/src/jvm/test.kt
            |actual annotation class A actual constructor(actual val name: String)
        """.trimIndent(), multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page =
                    module.children.single { it.name == "[root]" }
                        .children.single { it.name == "A" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"A" }
                        platformHinted {
                            check {
                                assertEquals(setOf("common", "jvm"), sourceSets.map(DisplaySourceSet::name).toSet())
                            }
                            group {
                                check {
                                    assertEquals("common", this.sourceSets.single().name)
                                }
                                +"expect annotation class "
                                link { +"A" }
                                +"("
                                group {
                                    group {
                                        +"val name: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            group {
                                check {
                                    assertEquals("jvm", this.sourceSets.single().name)
                                }
                                +"actual annotation class "
                                link { +"A" }
                                +"("
                                group {
                                    group {
                                        +"val name: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header(2) { +"Properties" }
                                table {
                                    group {
                                        link { +"name" }
                                        divergentGroup {
                                            divergentInstance {
                                                group {
                                                    group {
                                                        group {
                                                            +"expect val "
                                                            link { +"name" }
                                                            +": "
                                                            group {
                                                                link { +"String" }
                                                            }
                                                        }
                                                        group {
                                                            +"actual val "
                                                            link { +"name" }
                                                            +": "
                                                            group {
                                                                link { +"String" }
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
                        skipAllNotMatching()
                    }
                }
            }
        }
    }

    @Test
    fun `annotation class with implicit actual constructor`() {
        testInline(
            """
            |/src/common/test.kt
            |expect annotation class A(val name: String)
            |/src/jvm/test.kt
            |actual annotation class A(actual val name: String)
        """.trimIndent(), multiplatformConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page =
                    module.children.single { it.name == "[root]" }
                        .children.single { it.name == "A" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"A" }
                        platformHinted {
                            check {
                                assertEquals(setOf("common", "jvm"), sourceSets.map(DisplaySourceSet::name).toSet())
                            }
                            group {
                                check {
                                    assertEquals("common", this.sourceSets.single().name)
                                }
                                +"expect annotation class "
                                link { +"A" }
                                +"("
                                group {
                                    group {
                                        +"val name: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                            group {
                                check {
                                    assertEquals("jvm", this.sourceSets.single().name)
                                }
                                +"actual annotation class "
                                link { +"A" }
                                +"("
                                group {
                                    group {
                                        +"val name: "
                                        group { link { +"String" } }
                                    }
                                }
                                +")"
                            }
                        }
                    }
                    group {
                        group {
                            group {
                                header(2) { +"Properties" }
                                table {
                                    group {
                                        link { +"name" }
                                        divergentGroup {
                                            divergentInstance {
                                                group {
                                                    group {
                                                        group {
                                                            +"expect val "
                                                            link { +"name" }
                                                            +": "
                                                            group {
                                                                link { +"String" }
                                                            }
                                                        }
                                                        group {
                                                            +"actual val "
                                                            link { +"name" }
                                                            +": "
                                                            group {
                                                                link { +"String" }
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
                        skipAllNotMatching()
                    }
                }
            }
        }
    }
}
