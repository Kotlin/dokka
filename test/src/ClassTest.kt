package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*

public class ClassTest {
    Test fun emptyClass() {
        verifyModel("test/data/classes/emptyClass.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertEquals("<init>", members.single().name)
                assertTrue(links.none())
            }
        }
    }

    Test fun classWithConstructor() {
        verifyModel("test/data/classes/classWithConstructor.kt") { model ->
            with (model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(links.none())

                assertEquals(1, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals("", doc)
                    assertEquals(1, details.count())
                    with(details.elementAt(0)) {
                        assertEquals("name", name)
                        assertEquals("", doc)
                    }
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    Test fun classWithFunction() {
        verifyModel("test/data/classes/classWithFunction.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals("", doc)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("fn", name)
                    assertEquals("", doc)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }
}