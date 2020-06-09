package content.annotations

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.bareSignature
import utils.functionSignature
import utils.propertySignature


class ContentForAnnotationsTest : AbstractCoreTest() {


    private val testConfiguration = dokkaConfiguration {
        passes {
            pass {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
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
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    mapOf("Fancy" to emptySet()),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    "String",
                                    "abc" to ParamAttributes(mapOf("Fancy" to emptySet()), emptySet(), "String")
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    @Disabled
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
                    propertySignature(mapOf("Suppress" to setOf("names")), "", "", emptySet(), "val", "property", "Int")
                }
            }
        }
    }


    @Test
    fun `rich annotation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@MustBeDocumented
            |@Retention(AnnotationRetention.SOURCE)
            |@Target(AnnotationTarget.FIELD)
            |annotation class BugReport(
            |    val assignedTo: String = "[none]",
            |    val testCase: KClass<ABC> = ABC::class,
            |    val status: Status = Status.UNCONFIRMED,
            |    val ref: Reference = Reference(value = 1),
            |    val reportedBy: Array<Reference>,
            |    val showStopper: Boolean = false
            |) {
            |    enum class Status {
            |        UNCONFIRMED, CONFIRMED, FIXED, NOTABUG
            |    }
            |    class ABC
            |}
            |annotation class Reference(val value: Int)
            |
            |
            |@BugReport(
            |    assignedTo = "me",
            |    testCase = BugReport.ABC::class,
            |    status = BugReport.Status.FIXED,
            |    ref = Reference(value = 2),
            |    reportedBy = [Reference(value = 2), Reference(value = 4)],
            |    showStopper = true
            |)
            |val ltint: Int = 5
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(
                        mapOf(
                            "BugReport" to setOf(
                                "assignedTo",
                                "testCase",
                                "status",
                                "ref",
                                "reportedBy",
                                "showStopper"
                            )
                        ), "", "", emptySet(), "val", "ltint", "Int"
                    )
                }
            }
        }
    }
}