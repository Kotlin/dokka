package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BasePropertyTest(val analysisPlatform: Platform) {

    protected val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun valueProperty() {
        checkSourceExistsAndVerifyModel("testdata/properties/valueProperty.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/properties/variableProperty.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/properties/valuePropertyWithGetter.kt", defaultModelConfig) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/properties/variablePropertyWithAccessors.kt", defaultModelConfig) { model ->
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

    @Test fun propertyWithReceiver() {
        checkSourceExistsAndVerifyModel(
            "testdata/properties/propertyWithReceiver.kt",
            defaultModelConfig
        ) { model ->
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
        checkSourceExistsAndVerifyModel("testdata/properties/propertyOverride.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single { it.name == "Bar" }.members.single { it.name == "xyzzy"}) {
                assertEquals("xyzzy", name)
                val override = references(RefKind.Override).single().to
                assertEquals("xyzzy", override.name)
                assertEquals("Foo", override.owner!!.name)
            }
        }
    }

    @Test fun sinceKotlin() {
        checkSourceExistsAndVerifyModel("testdata/properties/sinceKotlin.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("1.1", sinceKotlin)
            }
        }
    }
}

class JSPropertyTest: BasePropertyTest(Platform.js) {}

class JVMPropertyTest : BasePropertyTest(Platform.jvm) {
    @Test
    fun annotatedProperty() {
        checkSourceExistsAndVerifyModel(
            "testdata/properties/annotatedProperty.kt",
            modelConfig = ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        ) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals(1, annotations.count())
                with(annotations[0]) {
                    Assert.assertEquals("Strictfp", name)
                    Assert.assertEquals(Content.Empty, content)
                    Assert.assertEquals(NodeKind.Annotation, kind)
                }
            }
        }
    }

}

class CommonPropertyTest: BasePropertyTest(Platform.common) {}