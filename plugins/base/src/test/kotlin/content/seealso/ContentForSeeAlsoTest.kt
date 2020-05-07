package content.seealso
import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.pWrapped
import utils.functionSignature
import utils.unnamedTag

class ContentForSeeAlsoTest : AbstractCoreTest() {
    private val testConfiguration = dokkaConfiguration {
        passes {
            pass {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                targets = listOf("jvm")
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
                        functionSignature(emptySet(), "", "", emptySet(), "function", null, "abc" to mapOf("Type" to setOf("String")))
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
                            functionSignature(emptySet(), "", "", emptySet(), "function", null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"abc" }
                                group { }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function", null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"abc" }
                                group { +"Comment to abc" }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function",null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "kotlin.collections/Collection////"
                                link{ +"Collection"}
                                group { }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function",null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"Collection" }
                                group { +"Comment to stdliblink" }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function",null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("random comment")
                        unnamedTag("Author") { +"pikinier20" }
                        unnamedTag("Since") { +"0.11" }
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"Collection" }
                                group { +"Comment to stdliblink" }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function",null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"abc" }
                                group { +"Comment to abc2" }
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
                        functionSignature(emptySet(), "", "", emptySet(), "function",null, "abc" to mapOf("Type" to setOf("String")))
                    }
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"See also" }
                        table {
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"abc" }
                                group { +"Comment to abc1" }
                            }
                            group {
                                //DRI should be "test//abc/#/-1/"
                                link{ +"Collection" }
                                group { +"Comment to collection" }
                            }
                        }
                    }
                }
            }
        }
    }
}