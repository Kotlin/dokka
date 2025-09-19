/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.typealiases

import matchers.content.ContentMatcherBuilder
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
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.jetbrains.dokka.pages.ContentDivergentInstance
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentLink
import org.jetbrains.dokka.pages.ContentTable
import org.jetbrains.dokka.pages.MemberPage
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
                        table2("Constructors") {
                            group {
                                link { +"Foo" }
                                platformHinted {
                                    group {
                                        +"constructor()"
                                    }
                                }
                            }
                        }

                        table2("Types") {
                            element("A") {
                                divergentInstance {
                                    group4 {
                                        +"typealias "
                                        group { groupedLink { +"Foo.A" } }
                                        +" = "
                                        groupedLink { +"String" }

                                    }

                                    group4 { +"Brief text" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nested typealias in object`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |object Foo {
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
                                +"object "
                                link { +"Foo" }
                            }
                        }
                    }

                    table3("Types") {
                        element("A") {
                            divergentInstance {
                                group4 {
                                    +"typealias "
                                    group { groupedLink { +"Foo.A" } }
                                    +" = "
                                    groupedLink { +"String" }

                                }

                                group4 { +"Brief text" }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nested typealias in interface`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |interface Foo {
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
                                +"interface "
                                link { +"Foo" }
                            }
                        }
                    }

                    table3("Types") {
                        element("A") {
                            divergentInstance {
                                group4 {
                                    +"typealias "
                                    group { groupedLink { +"Foo.A" } }
                                    +" = "
                                    groupedLink { +"String" }
                                }
                                group4 { +"Brief text" }
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
                        group2 {
                            +"typealias "
                            group { groupedLink { +"Foo.A" } }
                            +" = "
                            groupedLink { +"String" }
                        }

                        group3 {
                            group { +"Brief text" }
                            group { +"some text" }
                        }

                        header { +"See also" }
                        table {
                            groupedLink { +"String" }
                        }

                        header { +"Throws" }
                        table {
                            group { groupedLink { +"Unit" } }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nested typealias use cases`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |interface Foo {
        |    typealias A = String
        |
        |    val property: A
        |
        |    fun A.extension(): Unit {}
        |    fun parameter(a: A): Unit {}
        |    fun returnValue(): A {
        |        return ""
        |    }
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
                                +"interface "
                                link { +"Foo" }
                            }
                        }
                    }

                    group2 {
                        table("Types") {
                            element("A") {
                                divergentInstance {
                                    group4 {
                                        +"typealias "
                                        group { groupedLink { +"Foo.A" } }
                                        +" = "
                                        groupedLink { +"String" }
                                    }
                                }
                            }
                        }

                        table("Properties") {
                            element("property") {
                                divergentInstance {
                                    group3 {
                                        +"abstract val "
                                        link { +"property" }
                                        +": "
                                        groupedLink { +"Foo.A" }
                                    }
                                }
                            }
                        }

                        table("Functions") {
                            element("extension") {
                                divergentInstance {
                                    group3 {
                                        +"open fun "
                                        groupedLink { +"Foo.A" }
                                        +"."
                                        link { +"extension" }
                                        +"()"
                                    }
                                }
                            }

                            element("parameter") {
                                divergentInstance {
                                    group3 {
                                        +"open fun "
                                        link { +"parameter" }
                                        +"("
                                        group2 {
                                            +"a: "
                                            groupedLink { +"Foo.A" }
                                        }
                                        +")"
                                    }
                                }
                            }

                            element("returnValue") {
                                divergentInstance {
                                    group3 {
                                        +"open fun "
                                        link { +"returnValue" }
                                        +"(): "
                                        groupedLink { +"Foo.A" }
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
    fun `nested typealias use in KDoc`() {
        testInline(
            """
        |/src/main/kotlin/test/test.kt
        |package example
        |
        |interface Foo {
        |    typealias A = String
        |
        |    /**
        |    * Link to [A]
        |    * 
        |    * @see A
        |    * @throws A
        |    */
        |    val property: A
        |}
        """,
            configuration
        ) {
            pagesTransformationStage = { module ->
                val page = module.dfs { it.name == "property" } as MemberPage
                page.content.assertNode {
                    group {
                        header(1) { +"property" }
                    }
                    divergentGroup {
                        divergentInstance {
                            group2 {
                                +"abstract val "
                                link { +"property" }
                                +": "
                                groupedLink { +"Foo.A" }
                            }

                            group {
                                group3 {
                                    +"Link to "
                                    link { +"A" }
                                }

                                header(4) { +"See also" }
                                table { groupedLink { +"Foo.A" } }

                                header(4) { +"Throws" }
                                table { group { groupedLink { +"Foo.A" } } }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `nested typealiases in multiplatform`() {
        testInline(
            """
        |/src/common/kotlin/test/test.kt
        |package example
        |expect class A
        |/src/jvm/kotlin/test/test.kt
        |package example
        |actual class A { 
        |    typealias B = String
        |    class C
        |}
        |/src/native/kotlin/test/test.kt
        |package example
        |actual class A { 
        |    class B
        |    class C
        |}
        """, multiplatformConfiguration
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
                            group {
                                +"actual class "
                                link { +"A" }
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
                                    group {
                                        +"constructor()"
                                    }
                                }
                            }
                        }

                        table2("Types") {
                            element("B") {
                                divergentInstance {
                                    group3 {
                                        +"class "
                                        link { +"B" }
                                    }
                                }
                                divergentInstance {
                                    group4 {
                                        +"typealias "
                                        group { groupedLink { +"A.B" } }
                                        +" = "
                                        groupedLink { +"String" }
                                    }
                                }
                            }

                            element("C") {
                                divergentInstance {
                                    group3 {
                                        +"class "
                                        link { +"C" }
                                    }
                                }
                                divergentInstance {
                                    group3 {
                                        +"class "
                                        link { +"C" }
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

private fun ContentMatcherBuilder<*>.group2(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    group { group(block) }
}

private fun ContentMatcherBuilder<*>.group3(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    group { group2(block) }
}

private fun ContentMatcherBuilder<*>.group4(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    group { group3(block) }
}

private fun ContentMatcherBuilder<*>.table(
    title: String,
    headerSize: Int = 2,
    block: ContentMatcherBuilder<ContentTable>.() -> Unit
) {
    group {
        header(headerSize) { +title }
        table {
            block()
        }
    }
}

private fun ContentMatcherBuilder<*>.table2(
    title: String,
    headerSize: Int = 2,
    block: ContentMatcherBuilder<ContentTable>.() -> Unit
) {
    group2 {
        header(headerSize) { +title }
        table {
            block()
        }
    }
}

private fun ContentMatcherBuilder<*>.table3(
    title: String,
    headerSize: Int = 2,
    block: ContentMatcherBuilder<ContentTable>.() -> Unit
) {
    group3 {
        header(headerSize) { +title }
        table {
            block()
        }
    }
}

private fun ContentMatcherBuilder<*>.element(
    name: String,
    block: ContentMatcherBuilder<ContentDivergentGroup>.() -> Unit
) {
    group {
        link { +name }

        divergentGroup {
            block()
        }
    }

}

private fun ContentMatcherBuilder<*>.groupedLink(block: ContentMatcherBuilder<ContentLink>.() -> Unit) {
    group { link(block) }
}