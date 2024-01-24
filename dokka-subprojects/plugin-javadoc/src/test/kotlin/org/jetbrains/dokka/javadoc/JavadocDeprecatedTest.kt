/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.DeprecatedPage
import org.jetbrains.dokka.javadoc.renderer.TemplateMap
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

internal class JavadocDeprecatedTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun `generates correct number of sections`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            @Suppress("UNCHECKED_CAST")
            assertEquals(6, (templateMap["sections"] as List<TemplateMap>).size)
        }
    }

    @Test
    fun `finds correct number of element for removal`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("For Removal")
            assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated constructors`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Constructors")
            assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated classes`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Classes")
            assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated enums`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Enums")
            assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated exceptions`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Exceptions")
            assertEquals(2, map.elements().size)
        }
    }

    @Tag("onlyDescriptors") // https://github.com/Kotlin/dokka/issues/3266 - `describeConstable` is in deprecated page on Java 17
    @Test
    fun `finds correct number of deprecated methods`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            //We are checking whether we will have an additional function for enum classes
            fun hasAdditionalFunction() =
                AnnotationTarget.ANNOTATION_CLASS::class.java.methods.any { it.name == "describeConstable" }

            val map = templateMap.section("Methods")
            assertEquals(if (hasAdditionalFunction()) 5 else 4, map.elements().size)
        }
    }

    @Test
    fun `should be sorted by position`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            @Suppress("UNCHECKED_CAST")
            val contents = (templateMap["sections"] as List<TemplateMap>).map { it["caption"] }

            // maybe some other ordering is required by the javadoc spec
            // but it has to be deterministic
            val expected = "Classes, Exceptions, Methods, Constructors, Enums, For Removal"
            val actual = contents.joinToString(separator = ", ")

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `provides correct information for deprecated element`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Enums")
            map.elements().first().let { element ->
                assertEquals("package1.ClassCEnum", element["name"])
                assertEquals("package1/ClassCEnum.html", element["address"])
                assertEquals("Documentation for ClassCEnum", element["description"])
            }
        }
    }

    private val query = """
            /src/source0.kt
            package package0
            /** 
            * Documentation for ClassA 
            */
            @Deprecated("Bo tak")
            class ClassA {
                fun a() {}
                @Deprecated("Bo tak")
                fun b() {}
                fun c() {}
            }
            
            /src/source1.kt
            package package1
            /**
            * Documentation for ClassB
            */
            class ClassB {
                fun d() {}
                @Deprecated("Bo tak")
                fun e() {}
                @Deprecated("Bo tak")
                fun f() {}
            }
            
            /src/source2.kt
            package package1
            /**
            * Documentation for ClassB
            */
            class ClassC {
                fun g() {}
                fun h() {}
                fun j() {}
                
                class InnerClass {
                    fun k() {}
                }
            }

            /src/source3.kt
            package package1
            /**
            * Documentation for ClassCEnum
            */
            @Deprecated("Bo tak")
            enum class ClassCEnum {
                A, D, E
            }

            /src/source4.java
            package package1;
            /**
            * Documentation for ClassJava
            */
            public class ClassJava {
                @Deprecated
                public ClassJava() {}
                @Deprecated(forRemoval = true)
                public void deprecatedMethod() {}
            }

            /src/source5.java
            package package1;
            /**
            * Documentation for ClassJavaException
            */
            @Deprecated
            public class ClassJavaException extends Exception { }

            /src/source6.kt
            package package1
            /**
            * Documentation for ClassKotlinException
            */
            @Deprecated
            class ClassKotlinException: Exception() {}
        """.trimIndent()

    private fun testDeprecatedPageTemplateMaps(operation: (TemplateMap) -> Unit) =
        testTemplateMapInline(query) {
            operation(firstPageOfType<DeprecatedPage>().templateMap)
        }

    @Suppress("UNCHECKED_CAST")
    private fun TemplateMap.section(name: String) =
        (this["sections"] as List<TemplateMap>).first { it["caption"] == name }

    @Suppress("UNCHECKED_CAST")
    private fun TemplateMap.elements() =
        this["elements"] as List<TemplateMap>
}
