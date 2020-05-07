package content.annotations

import matchers.content.assertNode
import matchers.content.group
import matchers.content.header
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.functionSignature
import utils.propertySignature


class ContentForAnnotationsTest : AbstractCoreTest() {


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
    fun `function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
            |    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD
            |)
            |@Retention(AnnotationRetention.SOURCE)
            |@MustBeDocumented
            |annotation class Fancy
            |
            |
            |@Fancy
            |fun function(@Fancy abc: String): String {
            |    return "Hello, " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                        functionSignature(
                            setOf("@Fancy()"),
                            "",
                            "",
                            emptySet(),
                            "function",
                            "String",
                            "abc" to mapOf("Type" to setOf("String"), "Annotations" to setOf("@Fancy()"))
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `property`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@Suppress
            |val property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(setOf("@Suppress(names=(...))"), "", "", emptySet(), "val", "property", "Int")
                }
            }
        }
    }

}