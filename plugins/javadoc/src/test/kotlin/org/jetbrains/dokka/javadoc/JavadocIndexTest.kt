package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.IndexPage
import org.jetbrains.dokka.javadoc.renderer.TemplateMap
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class JavadocIndexTest : AbstractJavadocTemplateMapTest() {

    private val commonTestQuery = """
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

    @Test
    fun `generates correct number of index pages`() {
        testIndexPages(commonTestQuery) { indexPages ->
            assertEquals(12, indexPages.size)
        }
    }

    @Test
    fun `handles correct number of elements`() {
        //We are checking whether we will have an additional function for enum classes
        fun hasAdditionalFunction() =
            AnnotationTarget.ANNOTATION_CLASS::class.java.methods.any { it.name == "describeConstable" }

        testIndexPages(commonTestQuery) { indexPages ->
            assertEquals(if (hasAdditionalFunction()) 32 else 31, indexPages.sumBy { it.elements.size })
        }
    }

    @Test
    fun `templateMap for class index`() {
        testIndexPagesTemplateMaps(commonTestQuery) { templateMaps ->
            @Suppress("UNCHECKED_CAST")
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
        testIndexPagesTemplateMaps(commonTestQuery) { templateMaps ->
            @Suppress("UNCHECKED_CAST")
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
        testIndexPagesTemplateMaps(commonTestQuery) { templateMaps ->
            @Suppress("UNCHECKED_CAST")
            val element = (templateMaps[0]["elements"] as List<TemplateMap>).first()
            assertEquals("../package0/ClassA.html#a()", element["address"])
            assertEquals("a()", element["name"])
            assertEquals("function", element["type"])
            assertEquals("&nbsp;", element["description"])
            assertEquals("package0.<a href=../package0/ClassA.html>ClassA</a>", element["origin"])

        }
    }

    @Test
    fun `should sort overloaded functions deterministically`() {
        val query = """
            /src/overloaded.kt
            package overloaded
           
            class Clazz {
                fun funName(param: List<String>) {}
                fun funName(param: String) {}
                fun funName(param: Map<String>) {}
                fun funName(param: Int) {}
            }
        """.trimIndent()

        testIndexPages(query) { allPages ->
            val indexPage = allPages.find { it.elements.any { el -> el.getId() == "funName" } }
            assertNotNull(indexPage) { "Index page with functions not found" }

            val indexElementDRIs = indexPage.elements.map { it.getDRI() }
            assertEquals(4, indexElementDRIs.size)
            indexElementDRIs.forEach {
                assertEquals("overloaded", it.packageName)
                assertEquals("Clazz", it.classNames)
                assertEquals("funName", it.callable!!.name)
                assertEquals(1, it.callable!!.params.size)
            }

            assertEquals("[kotlin.String]", indexElementDRIs[0].getParam(0))
            assertEquals("kotlin.Int", indexElementDRIs[1].getParam(0))
            assertEquals("kotlin.String", indexElementDRIs[2].getParam(0))
            assertEquals("kotlin.collections.List[kotlin.String]", indexElementDRIs[3].getParam(0))
        }
    }

    private fun DRI.getParam(index: Int) = this.callable!!.params[index].toString()

    private fun testIndexPages(query: String, operation: (List<IndexPage>) -> Unit) {
        testTemplateMapInline(query) {
            operation(allPagesOfType())
        }
    }

    private fun testIndexPagesTemplateMaps(query: String, operation: (List<TemplateMap>) -> Unit) =
        testTemplateMapInline(query) {
            operation(allPagesOfType<IndexPage>().map { it.templateMap })
        }
}