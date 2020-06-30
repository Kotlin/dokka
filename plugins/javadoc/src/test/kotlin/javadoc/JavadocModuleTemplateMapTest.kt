package javadoc

import javadoc.pages.JavadocModulePageNode
import javadoc.pages.RowJavadocListEntry
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import testApi.utils.assertIsInstance

internal class JavadocModuleTemplateMapTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun singleEmptyClass() {
        dualTestTemplateMapInline(
            kotlin =
            """
            /src/source.kt
            package com.test.package
            class Test
            """,
            java =
            """
            /src/com/test/package/Source.java
            package com.test.package;
            public class Test { }
            """
        ) {
            val moduleTemplateMap = singlePageOfType<JavadocModulePageNode>().templateMap
            assertEquals("main", moduleTemplateMap["kind"])
            assertEquals("root", moduleTemplateMap["title"])
            assertEquals("", moduleTemplateMap["subtitle"])
            assertEquals("Packages", moduleTemplateMap["tabTitle"])
            assertEquals("Package", moduleTemplateMap["colTitle"])
            assertEquals("", moduleTemplateMap["pathToRoot"])

            val list = moduleTemplateMap["list"] as List<*>
            assertEquals(1, list.size, "Expected only one entry in 'list'")
            val rowListEntry = assertIsInstance<RowJavadocListEntry>(list.first())

            assertEquals("com.test", rowListEntry.link.name)
            assertEquals(DRI("com.test"), rowListEntry.link.dri.single())
        }
    }

    @Test
    fun multiplePackages() {
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
            val moduleTemplateMap = singlePageOfType<JavadocModulePageNode>().templateMap
            val list = assertIsInstance<List<*>>(moduleTemplateMap["list"])
            assertEquals(2, list.size, "Expected two entries in 'list'")
            assertEquals("com.test.package0", assertIsInstance<RowJavadocListEntry>(list[0]).link.name)
            assertEquals("com.test.package1", assertIsInstance<RowJavadocListEntry>(list[1]).link.name)
            assertEquals(DRI("com.test.package0"), assertIsInstance<RowJavadocListEntry>(list[0]).link.dri.single())
            assertEquals(DRI("com.test.package1"), assertIsInstance<RowJavadocListEntry>(list[1]).link.dri.single())
        }
    }
}
