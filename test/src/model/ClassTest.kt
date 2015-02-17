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

    Test fun classWithDefaultObject() {
        verifyModel("test/data/classes/classWithDefaultObject.kt") { model ->
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
                    assertEquals(DocumentationNode.Kind.DefaultObjectProperty, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(2)) {
                    assertEquals("foo", name)
                    assertEquals(DocumentationNode.Kind.DefaultObjectFunction, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    Test fun annotatedClass() {
        verifyModel("test/data/classes/annotatedClass.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("data", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Annotation, kind)
                }
            }
        }
    }

    Test fun annotatedClassWithAnnotationParameters() {
        verifyModel("test/data/classes/annotatedClassWithAnnotationParameters.kt") { model ->
            with(model.members.single().members.single()) {
                with(deprecation!!) {
                    assertEquals("deprecated", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Annotation, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(1, details.count())
                        with(details[0]) {
                            assertEquals(DocumentationNode.Kind.Value, kind)
                            assertEquals("\"should no longer be used\"", name)
                        }
                    }
                }
            }
        }
    }

    Test fun javaAnnotationClass() {
        verifyModel("test/data/classes/javaAnnotationClass.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("Retention", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(DocumentationNode.Kind.Annotation, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals(1, details.count())
                        with(details[0]) {
                            assertEquals(DocumentationNode.Kind.Value, kind)
                            assertEquals("RetentionPolicy.SOURCE", name)
                        }
                    }
                }
            }
        }
    }

    Test fun notOpenClass() {
        verifyModel("test/data/classes/notOpenClass.kt") { model ->
            with(model.members.single().members.first { it.name == "D"}.members.first { it.name == "f" }) {
                val modifiers = details(DocumentationNode.Kind.Modifier)
                assertEquals(2, modifiers.size())
                assertEquals("final", modifiers[1].name)

                val overrideReferences = references(DocumentationReference.Kind.Override)
                assertEquals(1, overrideReferences.size())
            }
        }
    }

    Test fun indirectOverride() {
        verifyModel("test/data/classes/indirectOverride.kt") { model ->
            with(model.members.single().members.first { it.name == "E"}.members.first { it.name == "foo" }) {
                val modifiers = details(DocumentationNode.Kind.Modifier)
                assertEquals(2, modifiers.size())
                assertEquals("final", modifiers[1].name)

                val overrideReferences = references(DocumentationReference.Kind.Override)
                assertEquals(1, overrideReferences.size())
            }
        }
    }

    Test fun innerClass() {
        verifyPackageMember("test/data/classes/innerClass.kt") { cls ->
            val innerClass = cls.members.single { it.name == "D" }
            val modifiers = innerClass.details(DocumentationNode.Kind.Modifier)
            assertEquals(3, modifiers.size())
            assertEquals("inner", modifiers[2].name)
        }
    }
}
