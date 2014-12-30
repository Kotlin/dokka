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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
                assertTrue(links.none())

                assertEquals(1, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertEquals(2, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    with(details.elementAt(1)) {
                        assertEquals("name", name)
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(Content.Empty, content)
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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertEquals(1, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("fn", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals("Unit", detail(DocumentationNode.Kind.Type).name)
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
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Constructor, kind)
                    assertEquals(1, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("name", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Property, kind)
                    assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    Test fun classWithClassObject() {
        verifyModel("test/data/classes/classWithClassObject.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertEquals(2, details.count())
                assertTrue(links.none())

                assertEquals(3, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                }
                with(members.elementAt(1)) {
                    assertEquals("x", name)
                    assertEquals(DocumentationNode.Kind.ClassObjectProperty, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(2)) {
                    assertEquals("foo", name)
                    assertEquals(DocumentationNode.Kind.ClassObjectFunction, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }
}
