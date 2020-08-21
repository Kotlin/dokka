package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.DeprecatedPage
import org.jetbrains.dokka.javadoc.renderer.TemplateMap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class JavadocDeprecatedTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun `generates correct number of sections`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            Assertions.assertEquals(6, (templateMap["sections"] as List<TemplateMap>).size)
        }
    }

    @Test
    fun `finds correct number of element for removal`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("For Removal")
            Assertions.assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated constructors`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Constructors")
            Assertions.assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated classes`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Classes")
            Assertions.assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated enums`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Enums")
            Assertions.assertEquals(1, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated exceptions`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Exceptions")
            Assertions.assertEquals(2, map.elements().size)
        }
    }

    @Test
    fun `finds correct number of deprecated methods`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            //We are checking whether we will have an additional function for enum classes
            fun hasAdditionalFunction() =
                AnnotationTarget.ANNOTATION_CLASS::class.java.methods.any { it.name == "describeConstable" }

            val map = templateMap.section("Methods")
            Assertions.assertEquals(if (hasAdditionalFunction()) 5 else 4, map.elements().size)
        }
    }

    @Test
    fun `provides correct information for deprecated element`() {
        testDeprecatedPageTemplateMaps { templateMap ->
            val map = templateMap.section("Enums")
            map.elements().first().let { element ->
                Assertions.assertEquals("package1.ClassCEnum", element["name"])
                Assertions.assertEquals("package1/ClassCEnum.html", element["address"])
                Assertions.assertEquals("Documentation for ClassCEnum", element["description"])
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

    private fun TemplateMap.section(name: String) =
        (this["sections"] as List<TemplateMap>).first { it["caption"] == name }

    private fun TemplateMap.elements() =
        this["elements"] as List<TemplateMap>
}