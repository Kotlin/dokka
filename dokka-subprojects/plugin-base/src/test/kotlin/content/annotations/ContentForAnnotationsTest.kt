/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.annotations

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.utils.firstNotNullOfOrNull
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.PackagePageNode
import utils.ParamAttributes
import utils.assertNotNull
import utils.bareSignature
import utils.propertySignature
import kotlin.test.Test
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
            |@Target(AnnotationTarget.PROPERTY)
            |annotation class BugReport(
            |    val assignedTo: String = "[none]",
            |    val testCase: KClass<ABC> = ABC::class,
            |    val status: Status = Status.UNCONFIRMED,
            |    val ref: Reference = Reference(value = 1),
            |    val reportedBy: Array<Reference>,
            |    val showStopper: Boolean = false
            |    val previousReport: BugReport?
            |) {
            |    enum class Status {
            |        UNCONFIRMED, CONFIRMED, FIXED, NOTABUG
            |    }
            |    class ABC
            |}
            |annotation class Reference(val value: Long)
            |annotation class ReferenceReal(val value: Double)
            | 
            |
            |@BugReport(
            |    assignedTo = "me",
            |    testCase = BugReport.ABC::class,
            |    status = BugReport.Status.FIXED,
            |    ref = Reference(value = 2u),
            |    reportedBy = [Reference(value = 2UL), Reference(value = 4L), 
            |                  ReferenceReal(value = 4.9), ReferenceReal(value = 2f)],
            |    showStopper = true,
            |    previousReport = null
            |)
            |val ltint: Int = 5
        """.trimIndent(), testConfiguration
        ) {
            documentablesCreationStage = { modules ->

                fun expectedAnnotationValue(name: String, value: AnnotationParameterValue) = AnnotationValue(Annotations.Annotation(
                    dri = DRI("test", name),
                    params = mapOf("value" to value),
                    scope = Annotations.AnnotationScope.DIRECT,
                    mustBeDocumented = false
                ))
                val property = modules.flatMap { it.packages }.flatMap { it.properties }.first()
                val annotation = property.extra[Annotations]?.let {
                    it.directAnnotations.entries.firstNotNullOfOrNull { (_, annotations) -> annotations.firstOrNull() }
                }
                val annotationParams = annotation?.params ?: emptyMap()

                assertEquals(expectedAnnotationValue("Reference", IntValue(2)), annotationParams["ref"])

                val reportedByParam = ArrayValue(listOf(
                    expectedAnnotationValue("Reference", LongValue(2)),
                    expectedAnnotationValue("Reference", LongValue(4)),
                    expectedAnnotationValue("ReferenceReal", DoubleValue(4.9)),
                    expectedAnnotationValue("ReferenceReal", FloatValue(2f))
                ))
                assertEquals(reportedByParam, annotationParams["reportedBy"])
                assertEquals(BooleanValue(true), annotationParams["showStopper"])
                assertEquals(NullValue, annotationParams["previousReport"])
            }

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
                                "showStopper",
                                "previousReport"
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
                    it.directAnnotations.entries.firstNotNullOfOrNull { (_, annotations) -> annotations.firstOrNull() }
                }
                val setterAnnotation = property.getter?.extra?.get(Annotations)?.let {
                    it.directAnnotations.entries.firstNotNullOfOrNull { (_, annotations) -> annotations.firstOrNull() }
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
