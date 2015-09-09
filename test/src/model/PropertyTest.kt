package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.DocumentationReference
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
                assertTrue(members.none())
            }
        }
    }

    Test fun variablePropertyWithAccessors() {
        verifyModel("test/data/properties/variablePropertyWithAccessors.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(DocumentationNode.Kind.Property, kind)
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                val modifiers = details(DocumentationNode.Kind.Modifier).map { it.name }
                assertTrue("final" in modifiers)
                assertTrue("public" in modifiers)
                assertTrue("var" in modifiers)
                assertTrue(links.none())
                assertTrue(members.none())
            }
        }
    }

    Test fun annotatedProperty() {
        verifyModel("test/data/properties/annotatedProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("Volatile", name)
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
