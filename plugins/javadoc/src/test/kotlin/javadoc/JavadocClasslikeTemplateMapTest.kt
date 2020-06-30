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
            assertEquals("public final class <a href=TestClass.html>TestClass</a>", map["signature"])
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
            assertEquals("public final class <a href=TestClass.html>TestClass</a>", map["signature"])

            val methods = assertIsInstance<Map<Any, Any?>>(map["methods"])
            val ownMethods = assertIsInstance<List<*>>(methods["own"])
            assertEquals(1, ownMethods.size, "Expected only one method")
            val method = assertIsInstance<Map<Any, Any?>>(ownMethods.single())
            assertEquals("Documentation for testFunction", method["brief"])
            assertEquals("testFunction", method["name"])
            assertEquals(
                0, assertIsInstance<List<*>>(method["parameters"]).size,
                "Expected no parameters"
            )
            assertEquals(
                "final <a href=.html>String</a> <a href=.html>testFunction</a>()", method["signature"]
            )
        }
    }
}
