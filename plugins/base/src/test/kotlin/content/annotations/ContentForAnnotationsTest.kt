package content.annotations

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.assertNotNull
import utils.bareSignature
import utils.propertySignature
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ContentForAnnotationsTest : BaseAbstractTest() {


    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                classpath += jvmStdlibPath!!
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
                    propertySignature(emptyMap(), "", "", emptySet(), "val", "property", "Int", "6")
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
                    propertySignature(mapOf("Fancy" to emptySet()), "", "", emptySet(), "val", "property", "Int", "6")
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
                        ), "", "", emptySet(), "val", "ltint", "Int", "5"
                    )
                }
            }
        }
    }

    @Test
    fun `JvmName for property with setter and getter`() {
        testInline(
            """
                |/src/main/kotlin/test/source.kt
                |package test
                |@get:JvmName("xd")
                |@set:JvmName("asd")
                |var property: String
                |    get() = ""
                |    set(value) {}
            """.trimIndent(), testConfiguration
        ) {
            documentablesCreationStage = { modules ->
                fun expectedAnnotation(name: String) = Annotations.Annotation(
                    dri = DRI("kotlin.jvm", "JvmName"),
                    params = mapOf("name" to StringValue(name)),
                    scope = Annotations.AnnotationScope.DIRECT,
                    mustBeDocumented = true
                )

                val property = modules.flatMap { it.packages }.flatMap { it.properties }.first()
                val getterAnnotation = property.getter?.extra?.get(Annotations)?.let {
                    it.directAnnotations.entries.firstNotNullResult { (_, annotations) -> annotations.firstOrNull() }
                }
                val setterAnnotation = property.getter?.extra?.get(Annotations)?.let {
                    it.directAnnotations.entries.firstNotNullResult { (_, annotations) -> annotations.firstOrNull() }
                }

                assertEquals(expectedAnnotation("xd"), getterAnnotation)
                assertTrue(getterAnnotation?.mustBeDocumented!!)
                assertEquals(Annotations.AnnotationScope.DIRECT, getterAnnotation.scope)

                assertEquals(expectedAnnotation("asd"), setterAnnotation)
                assertTrue(setterAnnotation?.mustBeDocumented!!)
                assertEquals(Annotations.AnnotationScope.DIRECT, setterAnnotation.scope)
            }
        }
    }

    @Test
    fun `annotated bounds in Kotlin`() {
        testInline(
            """
             |/src/main/kotlin/test/source.kt
             |@MustBeDocumented
             |@Target(AnnotationTarget.TYPE_PARAMETER)
             |annotation class Hello(val bar: String)
             |fun <T: @Hello("abc") String> foo(arg: String): List<T> = TODO()
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { root ->
                val fooPage = root.dfs { it.name == "foo" } as MemberPageNode
                fooPage.content.dfs { it is ContentText && it.text == "Hello" }.assertNotNull()
            }
        }
    }

    @Test
    fun `annotated bounds in Java`() {
        testInline(
            """
             |/src/main/java/demo/AnnotationTest.java
             |package demo;
             |import java.lang.annotation.*;
             |import java.util.List;
             |@Documented
             |@Target({ElementType.TYPE_USE, ElementType.TYPE})
             |@interface Hello {
             |   public String bar() default "";
             |}
             |public class AnnotationTest {
             |    public <T extends @Hello(bar = "baz") String> List<T> foo() {
             |        return null;
             |    }
             |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { root ->
                val fooPage = root.dfs { it.name == "foo" } as MemberPageNode
                fooPage.content.dfs { it is ContentText && it.text == "Hello" }.assertNotNull()
            }
        }
    }
}
