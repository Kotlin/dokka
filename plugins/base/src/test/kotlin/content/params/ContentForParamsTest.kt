package content.params

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.*

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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null, "abc" to ParamAttributes(
                                        emptyMap(),
                                        emptySet(),
                                        "String"
                                    )
                                )
                            }
                        }
                    }
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                            }
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                unnamedTag("Author") { +"Kordyjan" }
                                unnamedTag("Since") { +"0.11" }
                            }
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                unnamedTag("Author") { +"Kordyjan" }
                                unnamedTag("Since") { +"0.11" }
                            }
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"abc"
                                                group { +"comment to param" }
                                            }
                                        }
                                    }
                                }
                            }
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { +"comment to first param" }
                                            }
                                            group {
                                                +"second"
                                                group { +"comment to second param" }
                                            }
                                            group {
                                                +"third"
                                                group { +"comment to third param" }
                                            }
                                        }
                                    }
                                }
                            }
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { +"comment to first param" }
                                            }
                                            group {
                                                +"second"
                                                group { +"comment to second param" }
                                            }
                                            group {
                                                +"third"
                                                group { +"comment to third param" }
                                            }
                                        }
                                    }
                                }
                            }
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"<receiver>"
                                                group { +"comment to receiver" }
                                            }
                                            group {
                                                +"abc"
                                                group { +"comment to param" }
                                            }
                                        }
                                    }
                                }
                            }
                            divergent {
                                bareSignatureWithReceiver(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "String",
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { +"comment to first param" }
                                            }
                                            group {
                                                +"third"
                                                group { +"comment to third param" }
                                            }
                                        }
                                    }
                                }
                            }
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
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
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            before {
                                pWrapped("comment to function")
                                unnamedTag("Author") { +"Kordyjan" }
                                unnamedTag("Since") { +"0.11" }
                                header(2) { +"Parameters" }

                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { +"comment to first param" }
                                            }
                                            group {
                                                +"second"
                                                group { +"comment to second param" }
                                            }
                                            group {
                                                +"third"
                                                group { +"comment to third param" }
                                            }
                                        }
                                    }
                                }
                            }
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}