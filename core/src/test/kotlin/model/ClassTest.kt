package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.RefKind
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BaseClassTest(val analysisPlatform: Platform) {
    protected val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun emptyClass() {
        checkSourceExistsAndVerifyModel("testdata/classes/emptyClass.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/emptyObject.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/classWithConstructor.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/classWithFunction.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/classWithProperty.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/classWithCompanionObject.kt", defaultModelConfig) { model ->
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
                    assertEquals("x", name)
                    assertEquals(NodeKind.CompanionObjectProperty, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members.elementAt(2)) {
                    assertEquals("foo", name)
                    assertEquals(NodeKind.CompanionObjectFunction, kind)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
            }
        }
    }

    @Test fun dataClass() {
        verifyPackageMember("testdata/classes/dataClass.kt", defaultModelConfig) { cls ->
            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
            assertTrue("data" in modifiers)
        }
    }

    @Test fun sealedClass() {
        verifyPackageMember("testdata/classes/sealedClass.kt", defaultModelConfig) { cls ->
            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
            assertEquals(1, modifiers.count { it == "sealed" })
        }
    }

    @Test fun annotatedClassWithAnnotationParameters() {
        checkSourceExistsAndVerifyModel(
            "testdata/classes/annotatedClassWithAnnotationParameters.kt",
            defaultModelConfig
        ) { model ->
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

    @Test fun notOpenClass() {
        checkSourceExistsAndVerifyModel("testdata/classes/notOpenClass.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/classes/indirectOverride.kt", defaultModelConfig) { model ->
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
        verifyPackageMember("testdata/classes/innerClass.kt", defaultModelConfig) { cls ->
            val innerClass = cls.members.single { it.name == "D" }
            val modifiers = innerClass.details(NodeKind.Modifier)
            assertEquals(3, modifiers.size)
            assertEquals("inner", modifiers[2].name)
        }
    }

    @Test fun companionObjectExtension() {
        checkSourceExistsAndVerifyModel("testdata/classes/companionObjectExtension.kt", defaultModelConfig) { model ->
            val pkg = model.members.single()
            val cls = pkg.members.single { it.name == "Foo" }
            val extensions = cls.extensions.filter { it.kind == NodeKind.CompanionObjectProperty }
            assertEquals(1, extensions.size)
        }
    }

    @Test fun secondaryConstructor() {
        verifyPackageMember("testdata/classes/secondaryConstructor.kt", defaultModelConfig) { cls ->
            val constructors = cls.members(NodeKind.Constructor)
            assertEquals(2, constructors.size)
            with (constructors.first { it.details(NodeKind.Parameter).size == 1}) {
                assertEquals("<init>", name)
                assertEquals("This is a secondary constructor.", summary.toTestString())
            }
        }
    }

    @Test fun sinceKotlin() {
        checkSourceExistsAndVerifyModel("testdata/classes/sinceKotlin.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("1.1", sinceKotlin)
            }
        }
    }

    @Test fun privateCompanionObject() {
        checkSourceExistsAndVerifyModel(
            "testdata/classes/privateCompanionObject.kt",
            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, includeNonPublic = false)
        ) { model ->
            with(model.members.single().members.single()) {
                assertEquals(0, members(NodeKind.CompanionObjectFunction).size)
                assertEquals(0, members(NodeKind.CompanionObjectProperty).size)
            }
        }
    }

}

class JSClassTest: BaseClassTest(Platform.js) {}

class JVMClassTest: BaseClassTest(Platform.jvm) {
    @Test
    fun annotatedClass() {
        verifyPackageMember("testdata/classes/annotatedClass.kt", ModelConfig(
            analysisPlatform = analysisPlatform,
            withKotlinRuntime = true
        )
        ) { cls ->
            Assert.assertEquals(1, cls.annotations.count())
            with(cls.annotations[0]) {
                Assert.assertEquals("Strictfp", name)
                Assert.assertEquals(Content.Empty, content)
                Assert.assertEquals(NodeKind.Annotation, kind)
            }
        }
    }


    @Test fun javaAnnotationClass() {
        checkSourceExistsAndVerifyModel(
            "testdata/classes/javaAnnotationClass.kt",
            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true)
        ) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals(1, annotations.count())
                with(annotations[0]) {
                    Assert.assertEquals("Retention", name)
                    Assert.assertEquals(Content.Empty, content)
                    Assert.assertEquals(NodeKind.Annotation, kind)
                    with(details[0]) {
                        Assert.assertEquals(NodeKind.Parameter, kind)
                        Assert.assertEquals(1, details.count())
                        with(details[0]) {
                            Assert.assertEquals(NodeKind.Value, kind)
                            Assert.assertEquals("RetentionPolicy.SOURCE", name)
                        }
                    }
                }
            }
        }
    }

}

class CommonClassTest: BaseClassTest(Platform.common) {}