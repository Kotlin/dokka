package javadoc

import javadoc.pages.JavadocClasslikePageNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import testApi.utils.assertIsInstance

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
            assertEquals("Documentation for TestClass", map["classlikeDocumentation"])
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
            assertEquals("Documentation for TestClass", map["classlikeDocumentation"])
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
            assertEquals("final <a href=.html>java.lang.String</a>", method.modifiers())
            assertEquals("<a href=.html>testFunction</a>()", method.signatureWithoutModifiers())
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
            assertEquals("@<a href=Author.html>Author</a>(name = \"Benjamin Franklin\")", signature["annotations"])

            val methods = assertIsInstance<Map<Any, Any?>>(map["methods"])
            val ownMethods = assertIsInstance<List<*>>(methods["own"])
            val method = assertIsInstance<Map<String, Any?>>(ownMethods.single())
            val methodSignature = assertIsInstance<Map<String, Any?>>(method["signature"])
            assertEquals("@<a href=Author.html>Author</a>(name = \"Franklin D. Roosevelt\")", methodSignature["annotations"])
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
            enum ClockDays {
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

            val (first, second) = entries
            assertEquals("Sample docs for first", first["brief"])
            assertEquals("Sample docs for second", second["brief"])

            assertEquals("<a href=.html>FIRST</a>", first.signatureWithoutModifiers())
            assertEquals("<a href=.html>SECOND</a>", second.signatureWithoutModifiers())
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
                expectedType = "<a href=.html>java.lang.String</a>",
                expectedDescription = "simple String parameter"
            )
            assertParameterNode(
                node = second,
                expectedName = "parameters",
                expectedType = "<a href=.html>java.lang.Integer</a>",
                expectedDescription = "simple Integer parameter"
            )
            assertParameterNode(
                node = third,
                expectedName = "list",
                expectedType = "<a href=.html>java.lang.Boolean</a>",
                expectedDescription = "simple Boolean parameter"
            )
        }
    }

    private fun assertParameterNode(node: Map<String, Any?>, expectedName: String, expectedType: String, expectedDescription: String){
        assertEquals(expectedName, node["name"])
        assertEquals(expectedType, node["type"])
        assertEquals(expectedDescription, node["description"])
    }

    private fun Map<String, Any?>.signatureWithModifiers(): String = "${modifiers()} ${signatureWithoutModifiers()}"

    private fun Map<String, Any?>.signatureWithoutModifiers(): String = (get("signature") as Map<String, Any?>)["signatureWithoutModifiers"] as String

    private fun Map<String, Any?>.modifiers(): String = (get("signature") as Map<String, Any?>)["modifiers"] as String

}
