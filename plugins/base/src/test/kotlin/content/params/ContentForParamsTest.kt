package content.params

import matchers.content.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.dfs
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import utils.*

class ContentForParamsTest : AbstractCoreTest() {
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

    @Test
    fun javaDocCommentWithDocumentedParameters() {
        testInline(
            """
            |/src/main/java/test/Main.java
            |package test
            | public class Main {
            |
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  */
            |   public void sample(String first, String second) {
            |  
            |   }
            | }
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val sampleFunction = module.dfs {
                    it is MemberPageNode && it.dri.first()
                        .toString() == "test/Main/sample/#java.lang.String#java.lang.String/PointingToDeclaration/"
                } as MemberPageNode
                val forJvm = (sampleFunction.documentable as DFunction).parameters.mapNotNull {
                    val jvm = it.documentation.keys.first { it.analysisPlatform == Platform.jvm }
                    it.documentation[jvm]
                }

                assert(forJvm.size == 2)
                val (first, second) = forJvm.map { it.paramsDescription() }
                assert(first == "comment to first param")
                assert(second == "comment to second param")
            }
        }
    }

    private fun DocumentationNode.paramsDescription(): String =
        children.firstIsInstanceOrNull<Param>()?.root?.children?.firstIsInstanceOrNull<Text>()?.body.orEmpty()

}
