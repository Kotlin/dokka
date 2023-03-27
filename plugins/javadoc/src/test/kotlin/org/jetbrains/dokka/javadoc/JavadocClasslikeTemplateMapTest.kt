package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.JavadocClasslikePageNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JavadocClasslikeTemplateMapTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun `empty class`() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            /**
            * Documentation for TestClass
            */
            class TestClass
            """,
            java =
            """
            /src/com/test/package0/TestClass.java
            package com.test.package0;
            /**
            * Documentation for TestClass
            */
            public final class TestClass {}
            """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            assertEquals("TestClass", map["name"])
            assertEquals("TestClass", map["title"])
            assertEquals("com.test.package0", map["packageName"])
            assertEquals("<p>Documentation for TestClass</p>", map["classlikeDocumentation"])
            assertEquals("Documentation for TestClass", map["subtitle"])
            assertEquals("public final class <a href=TestClass.html>TestClass</a>", map.signatureWithModifiers())
        }
    }

    @Test
    fun `single function`() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            /**
            * Documentation for TestClass
            */
            class TestClass {
                /**
                * Documentation for testFunction
                */
                fun testFunction(): String = ""
            }
            """,
            java =
            """
            /src/com/test/package0/TestClass.java
            package com.test.package0
            /**
            * Documentation for TestClass
            */
            public final class TestClass {
                /**
                * Documentation for testFunction
                */
                public final String testFunction() {
                    return "";
                }
            }
            """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap

            assertEquals("TestClass", map["name"])
            assertEquals("TestClass", map["title"])
            assertEquals("com.test.package0", map["packageName"])
            assertEquals("<p>Documentation for TestClass</p>", map["classlikeDocumentation"])
            assertEquals("Documentation for TestClass", map["subtitle"])
            assertEquals("public final class", map.modifiers())
            assertEquals("<a href=TestClass.html>TestClass</a>", map.signatureWithoutModifiers())

            val methods = assertIsInstance<Map<Any, Any?>>(map["methods"])
            val ownMethods = assertIsInstance<List<*>>(methods["own"])
            assertEquals(1, ownMethods.size, "Expected only one method")
            val method = assertIsInstance<Map<String, Any?>>(ownMethods.single())
            assertEquals("Documentation for testFunction", method["brief"])
            assertEquals("testFunction", method["name"])
            assertEquals(
                0, assertIsInstance<List<*>>(method["parameters"]).size,
                "Expected no parameters"
            )
            assertEquals("final <a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a>", method.modifiers())
            assertEquals("<a href=TestClass.html#testFunction()>testFunction</a>()", method.signatureWithoutModifiers())
        }
    }

    @Test
    fun `class with annotation`(){
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            @MustBeDocumented
            annotation class Author(val name: String)

            @Author(
                    name = "Benjamin Franklin"
            )
            class TestClass {`
                
                @Author(
                    name = "Franklin D. Roosevelt"
                )
                fun testFunction(): String = ""
            }
            """,
            java =
            """
            /src/com/test/package0/Author.java
            package com.test.package0
            import java.lang.annotation.Documented;

            @Documented
            public @interface Author {
                String name();
            }
            /src/com/test/package0/TestClass.java
            package com.test.package0

            @Author(
                    name = "Benjamin Franklin"
            )
            public final class TestClass {
                
                @Author(
                    name = "Franklin D. Roosevelt"
                )
                public final String testFunction() {
                    return "";
                }
            }
            """
        ){
            val map = allPagesOfType<JavadocClasslikePageNode>().first { it.name == "TestClass" }.templateMap
            assertEquals("TestClass", map["name"])
            val signature = assertIsInstance<Map<String, Any?>>(map["signature"])
            assertEquals("@<a href=Author.html>Author</a>(name = &quot;Benjamin Franklin&quot;)", signature["annotations"])

            val methods = assertIsInstance<Map<Any, Any?>>(map["methods"])
            val ownMethods = assertIsInstance<List<*>>(methods["own"])
            val method = assertIsInstance<Map<String, Any?>>(ownMethods.single())
            val methodSignature = assertIsInstance<Map<String, Any?>>(method["signature"])
            assertEquals("@<a href=Author.html>Author</a>(name = &quot;Franklin D. Roosevelt&quot;)", methodSignature["annotations"])
        }
    }

    @Test
    fun `simple enum`(){
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            enum class ClockDays {
                /**
                 * Sample docs for first
                 */
                FIRST,
                /**
                 * Sample docs for second
                 */
                SECOND
            }
            """,
            java =
            """
            /src/com/test/package0/TestClass.java
            package com.test.package0;
            public enum ClockDays {
                /**
                 * Sample docs for first
                 */
                FIRST,
                /**
                 * Sample docs for second
                 */
                SECOND
            }
            """
        ){
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            assertEquals("ClockDays", map["name"])
            assertEquals("enum", map["kind"])
            val entries = assertIsInstance<List<Map<String, Any?>>>(map["entries"])
            assertEquals(2, entries.size)

            val (first, second) = entries.sortedBy { it["brief"] as String }
            assertEquals("<p>Sample docs for first</p>", first["brief"])
            assertEquals("<p>Sample docs for second</p>", second["brief"])

            assertEquals("<a href=ClockDays.html#FIRST>FIRST</a>", first.signatureWithoutModifiers())
            assertEquals("<a href=ClockDays.html#SECOND>SECOND</a>", second.signatureWithoutModifiers())
        }
    }

    @Test
    fun `documented function parameters`(){
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            class TestClass {
                /**
                 * Simple parameters list to check out
                 * @param simple simple String parameter
                 * @param parameters simple Integer parameter
                 * @param list simple Boolean parameter
                 * @return just a String
                 */
                fun testFunction(simple: String?, parameters: Int?, list: Boolean?): String {
                    return ""
                }
            }
            """,
            java =
            """
            /src/com/test/package0/TestClass.java
            package com.test.package0;
            public final class TestClass {
                /**
                 * Simple parameters list to check out
                 * @param simple simple String parameter
                 * @param parameters simple Integer parameter
                 * @param list simple Boolean parameter
                 * @return just a String
                 */
                public final String testFunction(String simple, Integer parameters, Boolean list) {
                    return "";
                }
            }
            """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            assertEquals("TestClass", map["name"])

            val methods = assertIsInstance<Map<String, Any?>>(map["methods"])
            val testFunction = assertIsInstance<List<Map<String, Any?>>>(methods["own"]).single()
            assertEquals("Simple parameters list to check out", testFunction["brief"])

            val (first, second, third) = assertIsInstance<List<Map<String, Any?>>>(testFunction["parameters"])
            assertParameterNode(
                node = first,
                expectedName = "simple",
                expectedType = "<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a>",
                expectedDescription = "simple String parameter"
            )
            assertParameterNode(
                node = second,
                expectedName = "parameters",
                expectedType = "<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html>Integer</a>",
                expectedDescription = "simple Integer parameter"
            )
            assertParameterNode(
                node = third,
                expectedName = "list",
                expectedType = "<a href=https://docs.oracle.com/javase/8/docs/api/java/lang/Boolean.html>Boolean</a>",
                expectedDescription = "simple Boolean parameter"
            )
        }
    }

    @Test
    fun `with generic parameters`(){
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            import java.io.Serializable

            class Generic<T : Serializable?> {
                fun <D : T> sampleFunction(): D = TODO()
            }
            """,
            java =
            """
            /src/com/test/package0/Generic.java
            package com.test.package0;
            import java.io.Serializable;

            public final class Generic<T extends Serializable> {
                public final <D extends T> D sampleFunction(){
                    return null;
                }
            }
            """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            assertEquals("Generic", map["name"])

            assertEquals(
                "public final class <a href=Generic.html>Generic</a>&lt;T extends <a href=https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html>Serializable</a>&gt;",
                map.signatureWithModifiers()
            )
            val methods = assertIsInstance<Map<Any, Any?>>(map["methods"])
            val ownMethods = assertIsInstance<List<*>>(methods["own"]).first()
            val sampleFunction = assertIsInstance<Map<String, Any?>>(ownMethods)

            assertEquals("final &lt;D extends <a href=Generic.html>T</a>&gt; <a href=Generic.html#sampleFunction()>D</a> <a href=Generic.html#sampleFunction()>sampleFunction</a>()", sampleFunction.signatureWithModifiers())
        }
    }

    @Test
    fun `class with top-level const`() {
        dualTestTemplateMapInline(
            kotlin =
                """
                /src/Test.kt
                package com.test.package0
                
                const val TEST_VAL = "test"
                """,
            java =
                """
                /src/com/test/package0/TestKt.java
                package com.test.package0;
                
                public final class TestKt {
                    public static final String TEST_VAL = "test"; 
                }
                """
        ) {
            val map = singlePageOfType<JavadocClasslikePageNode>().templateMap
            val properties = assertIsInstance<List<*>>(map["properties"])
            val property = assertIsInstance<Map<String, Any?>>(properties.first())
            assertEquals("public final static <a href=https://docs.oracle.com/javase/8/docs/api/java/lang/String.html>String</a> <a href=TestKt.html#TEST_VAL>TEST_VAL</a>", "${property["modifiers"]} ${property["signature"]}")
        }
    }

    private fun assertParameterNode(node: Map<String, Any?>, expectedName: String, expectedType: String, expectedDescription: String){
        assertEquals(expectedName, node["name"])
        assertEquals(expectedType, node["type"])
        assertEquals(expectedDescription, node["description"])
    }

    private fun Map<String, Any?>.signatureWithModifiers(): String = "${modifiers()} ${signatureWithoutModifiers()}"

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.signatureWithoutModifiers(): String = (get("signature") as Map<String, Any?>)["signatureWithoutModifiers"] as String

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.modifiers(): String = (get("signature") as Map<String, Any?>)["modifiers"] as String

}
