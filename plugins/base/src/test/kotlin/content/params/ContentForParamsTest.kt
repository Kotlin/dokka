package content.params

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.pWrapped
import utils.signature
import utils.signatureWithReceiver

class ContentForParamsTest : AbstractCoreTest() {
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
                    header(1) { +"function" }
                    signature("function", null, "abc" to "String")
                }
            }
        }
    }

    @Test
    fun `undocumented parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
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
                    header(1) { +"function" }
                    signature("function", null, "abc" to "String")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented parameter and other tags without function comment`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @author Kordyjan
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
                    header(1) { +"function" }
                    signature("function", null, "abc" to "String")
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"Author" }
                        +"Kordyjan"
                        header(4) { +"Since" }
                        +"0.11"
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented parameter and other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @author Kordyjan
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
                    header(1) { +"function" }
                    signature("function", null, "abc" to "String")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Author" }
                        +"Kordyjan"
                        header(4) { +"Since" }
                        +"0.11"
                    }
                }
            }
        }
    }

    @Test
    fun `single parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param abc comment to param
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
                    header(1) { +"function" }
                    signature("function", null, "abc" to "String")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"abc"
                                +"comment to param"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiple parameters`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    header(1) { +"function" }
                    signature("function", null, "first" to "String", "second" to "Int", "third" to "Double")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"first"
                                +"comment to first param"
                            }
                            group {
                                +"second"
                                +"comment to second param"
                            }
                            group {
                                +"third"
                                +"comment to third param"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiple parameters without function description`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    header(1) { +"function" }
                    signature("function", null, "first" to "String", "second" to "Int", "third" to "Double")
                    header(3) { +"Description" }
                    platformHinted {
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"first"
                                +"comment to first param"
                            }
                            group {
                                +"second"
                                +"comment to second param"
                            }
                            group {
                                +"third"
                                +"comment to third param"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `function with receiver`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param abc comment to param
            |  * @receiver comment to receiver
            |  */
            |fun String.function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    header(1) { +"function" }
                    signatureWithReceiver("String", "function", null, "abc" to "String")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"<receiver>"
                                +"comment to receiver"
                            }
                            group {
                                +"abc"
                                +"comment to param"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `missing parameter documentation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    header(1) { +"function" }
                    signature("function", null, "first" to "String", "second" to "Int", "third" to "Double")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"first"
                                +"comment to first param"
                            }
                            group {
                                +"third"
                                +"comment to third param"
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `parameters mixed with other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @author Kordyjan
            |  * @param second comment to second param
            |  * @since 0.11
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    header(1) { +"function" }
                    signature("function", null, "first" to "String", "second" to "Int", "third" to "Double")
                    header(3) { +"Description" }
                    platformHinted {
                        pWrapped("comment to function")
                        header(4) { +"Parameters" }
                        table {
                            group {
                                +"first"
                                +"comment to first param"
                            }
                            group {
                                +"second"
                                +"comment to second param"
                            }
                            group {
                                +"third"
                                +"comment to third param"
                            }
                        }
                        header(4) { +"Author" }
                        +"Kordyjan"
                        header(4) { +"Since" }
                        +"0.11"
                    }
                }
            }
        }
    }
}