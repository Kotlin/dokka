package content.seealso

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentPage
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.bareSignature
import utils.comment
import utils.unnamedTag
import kotlin.test.assertEquals

class ContentForSeeAlsoTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `undocumented function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
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
    fun `documented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc" }
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
    fun `should use fully qualified name for unresolved link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see com.example.NonExistingClass description for non-existing
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        +"com.example.NonExistingClass"
                                        group {
                                            group { +"description for non-existing" }
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
    fun `undocumented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        link {
                                            check {
                                                assertEquals(
                                                    "kotlin.collections/Collection///PointingToDeclaration/",
                                                    (this as ContentDRILink).address.toString()
                                                )
                                            }
                                            +"Collection"
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
    fun `documented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection Comment to stdliblink
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group {
                                            group { +"Comment to stdliblink" }
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
    fun `documented seealso with stdlib link with other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * random comment
            |  * @see Collection Comment to stdliblink
            |  * @author pikinier20
            |  * @since 0.11
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                group { comment { +"random comment" } }
                                unnamedTag("Author") { comment { +"pikinier20" } }
                                unnamedTag("Since") { comment { +"0.11" } }

                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group {
                                            group { +"Comment to stdliblink" }
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
    fun `documented multiple see also`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see abc Comment to abc2
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc2" }
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
    fun `documented multiple see also mixed source`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see[Collection] Comment to collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
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
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc1" }
                                        }
                                    }
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group { group { +"Comment to collection" } }
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
    fun `should prefix static function and property links with class name`() {
        testInline(
            """
            |/src/main/kotlin/com/example/package/CollectionExtensions.kt
            |package com.example.util
            |
            |object CollectionExtensions {
            |    val property = "Hi"
            |    fun emptyList() {}
            |}
            |
            |/src/main/kotlin/com/example/foo.kt
            |package com.example
            |
            |import com.example.util.CollectionExtensions.property
            |import com.example.util.CollectionExtensions.emptyList
            |
            |/**
            | * @see [property] static property
            | * @see [emptyList] static emptyList
            | */
            |fun function() {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "com.example" }
                    .children.single { it.name == "function" } as ContentPage

                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "function",
                                    returnType = null,
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        link { +"CollectionExtensions.property" }
                                        group {
                                            group { +"static property" }
                                        }
                                    }
                                    group {
                                        link { +"CollectionExtensions.emptyList" }
                                        group {
                                            group { +"static emptyList" }
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
