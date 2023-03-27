package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.AllClassesPage
import org.jetbrains.dokka.javadoc.pages.LinkJavadocListEntry
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JavadocAllClassesTemplateMapTest : AbstractJavadocTemplateMapTest() {
    @Test
    fun `two classes from different packages`() {
        dualTestTemplateMapInline(
            """
            /src/source0.kt
            package package0
            /** 
            * Documentation for ClassA 
            */
            class ClassA
            
            /src/source1.kt
            package package1
            /**
            * Documentation for ClassB
            */
            class ClassB
            """
        ) {
            val map = singlePageOfType<AllClassesPage>().templateMap
            assertEquals("main", map["kind"])
            assertEquals("All Classes", map["title"])

            val list = assertIsInstance<List<*>>(map["list"])
            assertEquals(2, list.size, "Expected two classes")

            val classA = assertIsInstance<LinkJavadocListEntry>(list[0])
            assertEquals("ClassA", classA.name)
            assertEquals(DRI("package0", "ClassA"), classA.dri.single())
            assertEquals(ContentKind.Classlikes, classA.kind)

            val classB = assertIsInstance<LinkJavadocListEntry>(list[1])
            assertEquals("ClassB", classB.name)
            assertEquals(DRI("package1", "ClassB"), classB.dri.single())
            assertEquals(ContentKind.Classlikes, classB.kind)
        }
    }
}
