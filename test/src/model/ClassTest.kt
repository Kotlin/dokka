package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class ClassTest {
    Test fun emptyClass() {
        verifyModel("test/data/classes/emptyClass.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertEquals("<init>", members.single().name)
                assertTrue(links.none())
            }
        }
    }

    Test fun gnClass() {
        verifyModel("test/data/classes/emptyClass.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertEquals("<init>", members.single().name)
                assertTrue(links.none())
            }
        }
    }

    Test fun emptyObject() {
        verifyModel("test/data/classes/emptyObject.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Object, kind)
                assertEquals("Obj", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun classWithConstructor() {
        verifyModel("test/data/classes/classWithConstructor.kt") { model ->
            with (model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(links.none())

                assertEquals(1, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertEquals(1, details.count())
                    with(details.elementAt(0)) {
                        assertEquals("name", name)
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(DocumentationContent.Empty, doc)
                        assertEquals("String", details.single().name)
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
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertTrue(details.none())
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("fn", name)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals("Unit", details.single().name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }

    Test fun classWithProperty() {
        verifyModel("test/data/classes/classWithProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("name", name)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(DocumentationNode.Kind.Property, kind)
                    assertEquals("String", details.single().name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }
}