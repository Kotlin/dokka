package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.JavadocContentKind
import org.jetbrains.dokka.javadoc.pages.JavadocPackagePageNode
import org.jetbrains.dokka.javadoc.pages.RowJavadocListEntry
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.test.assertIsInstance
import java.io.File

internal class JavadocPackageTemplateMapTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun `single class`() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source.kt
            package com.test.package0
            class Test 
            """,
            java =
            """
            /src/com/test/package0/Test.java
            package com.test.package0;
            public class Test {} 
            """
        ) {
            val map = singlePageOfType<JavadocPackagePageNode>().templateMap
            assertEquals("Class Summary", ((map["lists"] as List<*>).first() as Map<String, *>)["tabTitle"])
            assertEquals("Class", ((map["lists"] as List<*>).first() as Map<String, *>)["colTitle"])
            assertEquals("Package com.test.package0", map["title"])
            assertEquals("", map["subtitle"])
            assertEquals("package", map["kind"])

            val list = assertIsInstance<List<*>>(((map["lists"] as List<*>).first() as Map<String, *>)["list"])
            val entry = assertIsInstance<RowJavadocListEntry>(list.single())
            assertEquals("Test", entry.link.name)
            assertEquals(JavadocContentKind.Class, entry.link.kind)
            assertEquals(DRI("com.test.package0", "Test"), entry.link.dri.single())
        }
    }

    @Test
    fun `multiple packages`() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source0.kt
            package com.test.package0
            class Test0
            
            /src/source1.kt
            package com.test.package1
            class Test1
            """,
            java =
            """
            /src/com/test/package0/Test0.java
            package com.test.package0;
            public class Test0 {}
            
            /src/com/test/package1/Test1.java
            package com.test.package1;
            public class Test1 {}
            """
        ) {
            val packagePages = allPagesOfType<JavadocPackagePageNode>()
            packagePages.forEach { page ->
                val map = page.templateMap
                assertEquals("Class Summary", ((map["lists"] as List<*>).first() as Map<String, *>)["tabTitle"])
                assertEquals("Class", ((map["lists"] as List<*>).first() as Map<String, *>)["colTitle"])
                assertEquals("", map["subtitle"])
                assertEquals("package", map["kind"])
            }

            assertEquals(2, packagePages.size, "Expected two package pages")
        }
    }

    @Disabled("To be implemented / To be fixed")
    @Test
    fun `single class with package documentation`() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/packages.md
            # Package com.test.package0
            ABC

            /src/source0.kt
            package com.test.package0
            class Test
            """,

            java =
            """
            /src/com/test/package0/package-info.java
            /**
            * ABC
            */
            package com.test.package0;
            
            /src/com/test/package0/Test.java
            package com.test.package0;
            public class Test{}
            """,
            configuration = config.copy(
                sourceSets = config.sourceSets.map { sourceSet ->
                    sourceSet.copy(
                        includes = setOf(File("packages.md"))
                    )
                }
            )
        ) {
            val packagePage = singlePageOfType<JavadocPackagePageNode>()

            val map = packagePage.templateMap
            assertEquals("ABD", map["subtitle"].toString().trim())
        }
    }
}
