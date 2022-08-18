package content.annotations

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.ContentPage
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.bareSignature

class SinceKotlinTest : BaseAbstractTest() {

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `function with since kotlin annotation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "ring" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"ring" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "ring",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
