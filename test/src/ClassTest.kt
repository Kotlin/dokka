package org.jetbrains.dokka.tests

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

    Test fun emptyObject() {
        verifyModel("test/data/classes/emptyObject.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Object, kind)
                assertEquals("Obj", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(members.none())
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
                    assertEquals(DocumentationNodeKind.Constructor, kind)
                    assertEquals(1, details.count())
                    with(details.elementAt(0)) {
                        assertEquals("name", name)
                        assertEquals(DocumentationNodeKind.Parameter, kind)
                        assertEquals("", doc)
                        assertTrue(details.none())
                        assertTrue(links.none())
                        assertTrue(members.none())
                    }
                    assertTrue(links.none())
                    assertTrue(members.none())
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
                    assertEquals(DocumentationNodeKind.Constructor, kind)
                    assertTrue(details.none())
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("fn", name)
                    assertEquals("", doc)
                    assertEquals(DocumentationNodeKind.Function, kind)
                    assertTrue(details.none())
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }

    Test fun classWithProperty() {
        verifyModel("test/data/classes/classWithProperty.kt") { model ->
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
                    assertEquals(DocumentationNodeKind.Constructor, kind)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("name", name)
                    assertEquals("", doc)
                    assertEquals(DocumentationNodeKind.Property, kind)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }
}