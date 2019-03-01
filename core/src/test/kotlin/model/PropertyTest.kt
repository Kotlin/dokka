package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.RefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class PropertyTest {
    @Test fun valueProperty() {
        verifyModel("testdata/properties/valueProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(NodeKind.Property, kind)
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(NodeKind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun variableProperty() {
        verifyModel("testdata/properties/variableProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(NodeKind.Property, kind)
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(NodeKind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun valuePropertyWithGetter() {
        verifyModel("testdata/properties/valuePropertyWithGetter.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(NodeKind.Property, kind)
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(NodeKind.Type).name)
                assertTrue(links.none())
                assertTrue(members.none())
            }
        }
    }

    @Test fun variablePropertyWithAccessors() {
        verifyModel("testdata/properties/variablePropertyWithAccessors.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("property", name)
                assertEquals(NodeKind.Property, kind)
                assertEquals(Content.Empty, content)
                assertEquals("String", detail(NodeKind.Type).name)
                val modifiers = details(NodeKind.Modifier).map { it.name }
                assertTrue("final" in modifiers)
                assertTrue("public" in modifiers)
                assertTrue("var" in modifiers)
                assertTrue(links.none())
                assertTrue(members.none())
            }
        }
    }

    @Test fun annotatedProperty() {
        verifyModel("testdata/properties/annotatedProperty.kt", withKotlinRuntime = true) { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("Strictfp", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Annotation, kind)
                }
            }
        }
    }

    @Test fun propertyWithReceiver() {
        verifyModel("testdata/properties/propertyWithReceiver.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("kotlin.String", name)
                assertEquals(NodeKind.ExternalClass, kind)
                with(members.single()) {
                    assertEquals("foobar", name)
                    assertEquals(NodeKind.Property, kind)
                }
            }
        }
    }

    @Test fun propertyOverride() {
        verifyModel("testdata/properties/propertyOverride.kt") { model ->
            with(model.members.single().members.single { it.name == "Bar" }.members.single { it.name == "xyzzy"}) {
                assertEquals("xyzzy", name)
                val override = references(RefKind.Override).single().to
                assertEquals("xyzzy", override.name)
                assertEquals("Foo", override.owner!!.name)
            }
        }
    }

    @Test fun sinceKotlin() {
        verifyModel("testdata/properties/sinceKotlin.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(listOf("Kotlin 1.1"), platforms)
            }
        }
    }
}
