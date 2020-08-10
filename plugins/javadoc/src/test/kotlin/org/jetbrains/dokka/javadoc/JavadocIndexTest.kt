package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.IndexPage
import org.jetbrains.dokka.javadoc.renderer.TemplateMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JavadocIndexTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun `generates correct number of index pages`() {
        testIndexPages { indexPages ->
            assertEquals(12, indexPages.size)
        }
    }

    @Test
    fun `handles correct number of elements`() {
        //We are checking whether we will have an additional function for enum classes
        fun hasAdditionalFunction() =
            AnnotationTarget.ANNOTATION_CLASS::class.java.methods.any { it.name == "describeConstable" }

        testIndexPages { indexPages ->
            assertEquals(if (hasAdditionalFunction()) 41 else 40, indexPages.sumBy { it.elements.size })
        }
    }

    @Test
    fun `templateMap for class index`() {
        testIndexPagesTemplateMaps { templateMaps ->
            val element = (templateMaps[2]["elements"] as List<TemplateMap>)[1]
            assertEquals("../package0/ClassA.html", element["address"])
            assertEquals("ClassA", element["name"])
            assertEquals("class", element["type"])
            assertEquals("Documentation for ClassA", element["description"])
            assertEquals("package0", element["origin"])

        }
    }

    @Test
    fun `templateMap for enum entry index`() {
        testIndexPagesTemplateMaps { templateMaps ->
            val element = (templateMaps[0]["elements"] as List<TemplateMap>).last()
            assertEquals("../package1/ClassCEnum.html#A", element["address"])
            assertEquals("A", element["name"])
            assertEquals("enum entry", element["type"])
            assertEquals("&nbsp;", element["description"])
            assertEquals("package1.<a href=../package1/ClassCEnum.html>ClassCEnum</a>", element["origin"])

        }
    }

    @Test
    fun `templateMap for function index`() {
        testIndexPagesTemplateMaps { templateMaps ->
            val element = (templateMaps[0]["elements"] as List<TemplateMap>).first()
            assertEquals("../package0/ClassA.html#a()", element["address"])
            assertEquals("a()", element["name"])
            assertEquals("function", element["type"])
            assertEquals("&nbsp;", element["description"])
            assertEquals("package0.<a href=../package0/ClassA.html>ClassA</a>", element["origin"])

        }
    }

    private val query = """
            /src/source0.kt
            package package0
            /** 
            * Documentation for ClassA 
            */
            class ClassA {
                fun a() {}
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
                fun e() {}
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
            enum class ClassCEnum {
                A, D, E
            }
        """.trimIndent()

    private fun testIndexPages(operation: (List<IndexPage>) -> Unit) {
        testTemplateMapInline(query) {
            operation(allPagesOfType())
        }
    }

    private fun testIndexPagesTemplateMaps(operation: (List<TemplateMap>) -> Unit) =
        testTemplateMapInline(query) {
            operation(allPagesOfType<IndexPage>().map { it.templateMap })
        }
}