package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*

public class ClassTest {
    Test fun emptyClass() {
        verifyModel("test/data/classes/emptyClass.kt") { model ->
            val item = model.nodes.single().members.single()
            assertEquals(DocumentationNodeKind.Class, item.kind)
            assertEquals("Klass", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertEquals("<init>", item.members.single().name)
            assertTrue(item.links.none())
        }
    }

    Test fun classWithConstructor() {
        verifyModel("test/data/classes/classWithConstructor.kt") { model ->
            val item = model.nodes.single().members.single()
            assertEquals(DocumentationNodeKind.Class, item.kind)
            assertEquals("Klass", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertEquals("<init>", item.members.single().name)
            assertTrue(item.links.none())
        }
    }

    Test fun classWithFunction() {
        verifyModel("test/data/classes/classWithFunction.kt") { model ->
            val item = model.nodes.single().members.single()
            assertEquals(DocumentationNodeKind.Class, item.kind)
            assertEquals("Klass", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertEquals("<init>", item.members.single().name)
            assertTrue(item.links.none())
        }
    }
}