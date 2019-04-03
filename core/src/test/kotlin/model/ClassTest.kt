package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.RefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassTest {
    @Test fun emptyClass() {
        verifyModel("testdata/classes/emptyClass.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertEquals("<init>", members.single().name)
                assertTrue(links.none())
            }
        }
    }

    @Test fun emptyObject() {
        verifyModel("testdata/classes/emptyObject.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NodeKind.Object, kind)
                assertEquals("Obj", name)
                assertEquals(Content.Empty, content)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun classWithConstructor() {
        verifyModel("testdata/classes/classWithConstructor.kt") { model ->
            with (model.members.single().members.single()) {
                assertEquals(NodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertTrue(links.none())

                assertEquals(1, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Constructor, kind)
                    assertEquals(3, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    with(details.elementAt(2)) {
                        assertEquals("name", name)
                        assertEquals(NodeKind.Parameter, kind)
                        assertEquals(Content.Empty, content)
                        assertEquals("String", detail(NodeKind.Type).name)
                        assertTrue(links.none())
                        assertTrue(members.none())
                    }
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }

    @Test fun classWithFunction() {
        verifyModel("testdata/classes/classWithFunction.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Constructor, kind)
                    assertEquals(2, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("fn", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Function, kind)
                    assertEquals("Unit", detail(NodeKind.Type).name)
                    assertTrue(links.none())
                    assertTrue(members.none())
                }
            }
        }
    }

    @Test fun classWithProperty() {
        verifyModel("testdata/classes/classWithProperty.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertTrue(links.none())

                assertEquals(2, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Constructor, kind)
                    assertEquals(2, details.count())
                    assertEquals("public", details.elementAt(0).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(1)) {
                    assertEquals("name", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Property, kind)
                    assertEquals("String", detail(NodeKind.Type).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    @Test fun classWithCompanionObject() {
        verifyModel("testdata/classes/classWithCompanionObject.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(NodeKind.Class, kind)
                assertEquals("Klass", name)
                assertEquals(Content.Empty, content)
                assertTrue(links.none())

                assertEquals(3, members.count())
                with(members.elementAt(0)) {
                    assertEquals("<init>", name)
                    assertEquals(Content.Empty, content)
                }
                with(members.elementAt(1)) {
                    assertEquals("foo", name)
                    assertEquals(NodeKind.CompanionObjectFunction, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(2)) {
                    assertEquals("x", name)
                    assertEquals(NodeKind.CompanionObjectProperty, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    @Test fun annotatedClass() {
        verifyPackageMember("testdata/classes/annotatedClass.kt", withKotlinRuntime = true) { cls ->
            assertEquals(1, cls.annotations.count())
            with(cls.annotations[0]) {
                assertEquals("Strictfp", name)
                assertEquals(Content.Empty, content)
                assertEquals(NodeKind.Annotation, kind)
            }
        }
    }

    @Test fun dataClass() {
        verifyPackageMember("testdata/classes/dataClass.kt") { cls ->
            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
            assertTrue("data" in modifiers)
        }
    }

    @Test fun sealedClass() {
        verifyPackageMember("testdata/classes/sealedClass.kt") { cls ->
            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
            assertEquals(1, modifiers.count { it == "sealed" })
        }
    }

    @Test fun annotatedClassWithAnnotationParameters() {
        verifyModel("testdata/classes/annotatedClassWithAnnotationParameters.kt") { model ->
            with(model.members.single().members.single()) {
                with(deprecation!!) {
                    assertEquals("Deprecated", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Annotation, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(NodeKind.Parameter, kind)
                        assertEquals(1, details.count())
                        with(details[0]) {
                            assertEquals(NodeKind.Value, kind)
                            assertEquals("\"should no longer be used\"", name)
                        }
                    }
                }
            }
        }
    }

    @Test fun javaAnnotationClass() {
        verifyModel("testdata/classes/javaAnnotationClass.kt", withJdk = true) { model ->
            with(model.members.single().members.single()) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("Retention", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Annotation, kind)
                    with(details[0]) {
                        assertEquals(NodeKind.Parameter, kind)
                        assertEquals(1, details.count())
                        with(details[0]) {
                            assertEquals(NodeKind.Value, kind)
                            assertEquals("RetentionPolicy.SOURCE", name)
                        }
                    }
                }
            }
        }
    }

    @Test fun notOpenClass() {
        verifyModel("testdata/classes/notOpenClass.kt") { model ->
            with(model.members.single().members.first { it.name == "D"}.members.first { it.name == "f" }) {
                val modifiers = details(NodeKind.Modifier)
                assertEquals(2, modifiers.size)
                assertEquals("final", modifiers[1].name)

                val overrideReferences = references(RefKind.Override)
                assertEquals(1, overrideReferences.size)
            }
        }
    }

    @Test fun indirectOverride() {
        verifyModel("testdata/classes/indirectOverride.kt") { model ->
            with(model.members.single().members.first { it.name == "E"}.members.first { it.name == "foo" }) {
                val modifiers = details(NodeKind.Modifier)
                assertEquals(2, modifiers.size)
                assertEquals("final", modifiers[1].name)

                val overrideReferences = references(RefKind.Override)
                assertEquals(1, overrideReferences.size)
            }
        }
    }

    @Test fun innerClass() {
        verifyPackageMember("testdata/classes/innerClass.kt") { cls ->
            val innerClass = cls.members.single { it.name == "D" }
            val modifiers = innerClass.details(NodeKind.Modifier)
            assertEquals(3, modifiers.size)
            assertEquals("inner", modifiers[2].name)
        }
    }

    @Test fun companionObjectExtension() {
        verifyModel("testdata/classes/companionObjectExtension.kt") { model ->
            val pkg = model.members.single()
            val cls = pkg.members.single { it.name == "Foo" }
            val extensions = cls.extensions.filter { it.kind == NodeKind.CompanionObjectProperty }
            assertEquals(1, extensions.size)
        }
    }

    @Test fun secondaryConstructor() {
        verifyPackageMember("testdata/classes/secondaryConstructor.kt") { cls ->
            val constructors = cls.members(NodeKind.Constructor)
            assertEquals(2, constructors.size)
            with (constructors.first { it.details(NodeKind.Parameter).size == 1}) {
                assertEquals("<init>", name)
                assertEquals("This is a secondary constructor.", summary.toTestString())
            }
        }
    }

    @Test fun sinceKotlin() {
        verifyModel("testdata/classes/sinceKotlin.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals(listOf("Kotlin 1.1"), platforms)
            }
        }
    }

    @Test fun privateCompanionObject() {
        verifyModel("testdata/classes/privateCompanionObject.kt", includeNonPublic = false) { model ->
            with(model.members.single().members.single()) {
                assertEquals(0, members(NodeKind.CompanionObjectFunction).size)
                assertEquals(0, members(NodeKind.CompanionObjectProperty).size)
            }
        }
    }

}
