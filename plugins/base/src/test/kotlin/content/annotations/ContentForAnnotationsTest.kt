package content.annotations

import matchers.content.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.bareSignature
import utils.propertySignature


class ContentForAnnotationsTest : AbstractCoreTest() {


    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `function with documented annotation`() {
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

    @Test
    fun `function with undocumented annotation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
            |    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD
            |)
            |@Retention(AnnotationRetention.SOURCE)
            |annotation class Fancy
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
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
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

    @Test
    fun `property with undocumented annotation`() {
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
                    propertySignature(emptyMap(), "", "", emptySet(), "val", "property", "Int")
                }
            }
        }
    }

    @Test
    fun `property with documented annotation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@MustBeDocumented
            |annotation class Fancy
            |
            |@Fancy
            |val property: Int = 6
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" } as PackagePageNode
                page.content.assertNode {
                    propertySignature(mapOf("Fancy" to emptySet()), "", "", emptySet(), "val", "property", "Int")
                }
            }
        }
    }


    @Test
    fun `rich documented annotation`() {
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
