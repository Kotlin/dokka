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
                assertEquals(Content.Empty, content)
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
                assertEquals(Content.Empty, content)
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
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                assertTrue(links.none())
                with(members.single()) {
                    assertEquals("get", name)
                    assertEquals(DocumentationNode.Kind.PropertyAccessor, kind)
                    assertEquals(Content.Empty, content)
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
                assertEquals(Content.Empty, content)
                assertEquals(4, details.count())
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                val modifiers = details(DocumentationNode.Kind.Modifier).map { it.name }
                assertTrue("final" in modifiers)
                assertTrue("internal" in modifiers)
                assertTrue("var" in modifiers)
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("get", name)
                    assertEquals(DocumentationNode.Kind.PropertyAccessor, kind)
                    assertEquals(Content.Empty, content)
                    val get_modifiers = details(DocumentationNode.Kind.Modifier).map { it.name }
                    assertTrue("final" in get_modifiers)
                    assertTrue("internal" in get_modifiers)
                    assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("set", name)
                    assertEquals(DocumentationNode.Kind.PropertyAccessor, kind)
                    assertEquals(Content.Empty, content)
                    assertEquals(4, details.count())
                    assertEquals("Unit", detail(DocumentationNode.Kind.Type).name)
                    val set_modifiers = details(DocumentationNode.Kind.Modifier).map { it.name }
                    assertTrue("final" in set_modifiers)
                    assertTrue("internal" in set_modifiers)
                    with(detail(DocumentationNode.Kind.Parameter)) {
                        assertEquals("value", name)
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(Content.Empty, content)
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

    Test fun annotatedProperty() {
        verifyModel("test/data/properties/annotatedProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("inline", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Annotation, kind)
                }
            }
        }
    }

    Test fun propertyWithReceiver() {
        verifyModel("test/data/properties/propertyWithReceiver.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("String", name)
                assertEquals(DocumentationNode.Kind.ExternalClass, kind)
                with(members.single()) {
                    assertEquals("foobar", name)
                    assertEquals(DocumentationNode.Kind.Property, kind)
                }
            }
        }
    }

    Test fun propertyOverride() {
        verifyModel("test/data/properties/propertyOverride.kt") { model ->
            with(model.members.single().members.single { it.name == "Bar" }.members.single { it.name == "xyzzy"}) {
                assertEquals("xyzzy", name)
                val override = references(DocumentationReference.Kind.Override).single().to
                assertEquals("xyzzy", override.name)
                assertEquals("Foo", override.owner!!.name)
            }
        }
    }
}
