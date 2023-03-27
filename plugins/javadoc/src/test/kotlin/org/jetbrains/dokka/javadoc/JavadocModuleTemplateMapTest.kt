package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.JavadocModulePageNode
import org.jetbrains.dokka.javadoc.pages.RowJavadocListEntry
import org.jetbrains.dokka.links.DRI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

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
            """,
            configuration = config.copy(moduleVersion = "1.2.3-SNAPSHOT")
        ) {
            val moduleTemplateMap = singlePageOfType<JavadocModulePageNode>().templateMap
            assertEquals("1.2.3-SNAPSHOT", moduleTemplateMap["version"])
            val list = assertIsInstance<List<*>>(moduleTemplateMap["list"])
            assertEquals(2, list.size, "Expected two entries in 'list'")
            assertEquals("com.test.package0", assertIsInstance<RowJavadocListEntry>(list[0]).link.name)
            assertEquals("com.test.package1", assertIsInstance<RowJavadocListEntry>(list[1]).link.name)
            assertEquals(DRI("com.test.package0"), assertIsInstance<RowJavadocListEntry>(list[0]).link.dri.single())
            assertEquals(DRI("com.test.package1"), assertIsInstance<RowJavadocListEntry>(list[1]).link.dri.single())
        }
    }

    @Test
    fun `single class with module documentation (kotlin)`() {
        testTemplateMapInline(
            query =
            """
            /src/module.md
            # Module module1
            ABC
            
            /src/source0.kt
            package com.test.package0
            class Test
            """,
            configuration = config.copy(
                sourceSets = config.sourceSets.map { sourceSet ->
                    sourceSet.copy(
                        includes = setOf(File("src/module.md"))
                    )
                },
                moduleName = "module1"
            )
        ) {
            val modulePage = singlePageOfType<JavadocModulePageNode>()

            val map = modulePage.templateMap
            assertEquals("<p>ABC</p>", map["subtitle"].toString().trim())
        }
    }

    @Test
    fun `single class with long module documentation (kotlin)`() {
        testTemplateMapInline(
            query =
            """
            /src/module.md
            # Module module1
            Aliquam rerum est vel. Molestiae eos expedita animi repudiandae sed commodi.
            Omnis qui ducimus ut et perspiciatis sint.
            
            Veritatis nam eaque sequi laborum voluptas voluptate aut.
            
            /src/source0.kt
            package com.test.package0
            class Test
            """,
            configuration = config.copy(
                sourceSets = config.sourceSets.map { sourceSet ->
                    sourceSet.copy(
                        includes = setOf(File("src/module.md"))
                    )
                },
                moduleName = "module1"
            )
        ) {
            val modulePage = singlePageOfType<JavadocModulePageNode>()

            val map = modulePage.templateMap
            val expectedText = """
                <p>Aliquam rerum est vel. Molestiae eos expedita animi repudiandae sed commodi. 
                Omnis qui ducimus ut et perspiciatis sint.</p>
                <p>Veritatis nam eaque sequi laborum voluptas voluptate aut.</p>
            """.trimIndent().replace("\n", "")
            assertEquals(expectedText, map["subtitle"].toString().trim())
        }
    }
}
