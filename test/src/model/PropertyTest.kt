package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class PropertyTest {
    Test fun valueProperty() {
        verifyModel("test/data/properties/valueProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(DocumentationNode.Kind.Property, kind)
                assertEquals(DocumentationContent.Empty, doc)
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun variableProperty() {
        verifyModel("test/data/properties/variableProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(DocumentationNode.Kind.Property, kind)
                assertEquals(DocumentationContent.Empty, doc)
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun valuePropertyWithGetter() {
        verifyModel("test/data/properties/valuePropertyWithGetter.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(DocumentationNode.Kind.Property, kind)
                assertEquals(DocumentationContent.Empty, doc)
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                assertTrue(links.none())
                with(members.single()) {
                    assertEquals("<get-property>", name)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }

    Test fun variablePropertyWithAccessors() {
        verifyModel("test/data/properties/variablePropertyWithAccessors.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(DocumentationNode.Kind.Property, kind)
                assertEquals(DocumentationContent.Empty, doc)
                assertEquals(3, details.count())
                with(details.elementAt(0)) {
                    assertEquals(DocumentationNode.Kind.Type, kind)
                    assertEquals("String", name)
                }
                with(details.elementAt(1)) {
                    assertEquals(DocumentationNode.Kind.Modifier, kind)
                    assertEquals("final", name)
                }
                with(details.elementAt(2)) {
                    assertEquals(DocumentationNode.Kind.Modifier, kind)
                    assertEquals("internal", name)
                }
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<get-property>", name)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("<set-property>", name)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals(4, details.count())
                    assertEquals("Unit", details.elementAt(0).name)
                    assertEquals("final", details.elementAt(1).name)
                    assertEquals("internal", details.elementAt(2).name)
                    with(details.elementAt(3)) {
                        assertEquals("value", name)
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(DocumentationContent.Empty, doc)
                        assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                        assertTrue(links.none())
                        assertTrue(members.none())
                    }
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }
}
