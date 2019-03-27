package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Content
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BaseFunctionTest(val analysisPlatform: Platform) {
    protected val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun function() {
        checkSourceExistsAndVerifyModel("testdata/functions/function.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("fn", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("Function fn", content.summary.toTestString())
                assertEquals("Unit", detail(NodeKind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun functionWithReceiver() {
        checkSourceExistsAndVerifyModel("testdata/functions/functionWithReceiver.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("kotlin.String", name)
                assertEquals(NodeKind.ExternalClass, kind)
                assertEquals(2, members.count())
                with(members[0]) {
                    assertEquals("fn", name)
                    assertEquals(NodeKind.Function, kind)
                    assertEquals("Function with receiver", content.summary.toTestString())
                    assertEquals("public", details.elementAt(0).name)
                    assertEquals("final", details.elementAt(1).name)
                    with(details.elementAt(3)) {
                        assertEquals("<this>", name)
                        assertEquals(NodeKind.Receiver, kind)
                        assertEquals(Content.Empty, content)
                        assertEquals("String", details.single().name)
                        assertTrue(members.none())
                        assertTrue(links.none())
                    }
                    assertEquals("Unit", details.elementAt(4).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(members[1]) {
                    assertEquals("fn", name)
                    assertEquals(NodeKind.Function, kind)
                }
            }
        }
    }

    @Test fun genericFunction() {
        checkSourceExistsAndVerifyModel("testdata/functions/genericFunction.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("generic function", content.summary.toTestString())

                assertEquals("private", details.elementAt(0).name)
                assertEquals("final", details.elementAt(1).name)
                with(details.elementAt(3)) {
                    assertEquals("T", name)
                    assertEquals(NodeKind.TypeParameter, kind)
                    assertEquals(Content.Empty, content)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                assertEquals("Unit", details.elementAt(4).name)

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
    @Test fun genericFunctionWithConstraints() {
        checkSourceExistsAndVerifyModel("testdata/functions/genericFunctionWithConstraints.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("generic function", content.summary.toTestString())

                val functionDetails = details
                assertEquals("public", functionDetails.elementAt(0).name)
                assertEquals("final", functionDetails.elementAt(1).name)
                with(functionDetails.elementAt(3)) {
                    assertEquals("T", name)
                    assertEquals(NodeKind.TypeParameter, kind)
                    assertEquals(Content.Empty, content)
                    with(details.single()) {
                        assertEquals("R", name)
                        assertEquals(NodeKind.UpperBound, kind)
                        assertEquals(Content.Empty, content)
                        assertTrue(details.none())
                        assertTrue(members.none())
                        assertTrue(links.singleOrNull() == functionDetails.elementAt(4))
                    }
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(functionDetails.elementAt(4)) {
                    assertEquals("R", name)
                    assertEquals(NodeKind.TypeParameter, kind)
                    assertEquals(Content.Empty, content)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                assertEquals("Unit", functionDetails.elementAt(5).name)

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun functionWithParams() {
        checkSourceExistsAndVerifyModel("testdata/functions/functionWithParams.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("function", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("Multiline", content.summary.toTestString())
                assertEquals("""Function
Documentation""", content.description.toTestString())

                assertEquals("public", details.elementAt(0).name)
                assertEquals("final", details.elementAt(1).name)
                with(details.elementAt(3)) {
                    assertEquals("x", name)
                    assertEquals(NodeKind.Parameter, kind)
                    assertEquals("parameter", content.summary.toTestString())
                    assertEquals("Int", detail(NodeKind.Type).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                assertEquals("Unit", details.elementAt(4).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    @Test fun functionWithNotDocumentedAnnotation() {
        verifyPackageMember("testdata/functions/functionWithNotDocumentedAnnotation.kt", defaultModelConfig) { func ->
            assertEquals(0, func.annotations.count())
        }
    }

    @Test fun inlineFunction() {
        verifyPackageMember("testdata/functions/inlineFunction.kt", defaultModelConfig) { func ->
            val modifiers = func.details(NodeKind.Modifier).map { it.name }
            assertTrue("inline" in modifiers)
        }
    }

    @Test fun suspendFunction() {
        verifyPackageMember("testdata/functions/suspendFunction.kt") { func ->
            val modifiers = func.details(NodeKind.Modifier).map { it.name }
            assertTrue("suspend" in modifiers)
        }
    }

    @Test fun suspendInlineFunctionOrder() {
        verifyPackageMember("testdata/functions/suspendInlineFunction.kt") { func ->
            val modifiers = func.details(NodeKind.Modifier).map { it.name }.filter {
                it == "suspend" || it == "inline"
            }

            assertEquals(listOf("suspend", "inline"), modifiers)
        }
    }

    @Test fun inlineSuspendFunctionOrderChanged() {
        verifyPackageMember("testdata/functions/inlineSuspendFunction.kt") { func ->
            val modifiers = func.details(NodeKind.Modifier).map { it.name }.filter {
                it == "suspend" || it == "inline"
            }

            assertEquals(listOf("suspend", "inline"), modifiers)
        }
    }

    @Test fun functionWithAnnotatedParam() {
        checkSourceExistsAndVerifyModel("testdata/functions/functionWithAnnotatedParam.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single { it.name == "function" }) {
                with(details(NodeKind.Parameter).first()) {
                    assertEquals(1, annotations.count())
                    with(annotations[0]) {
                        assertEquals("Fancy", name)
                        assertEquals(Content.Empty, content)
                        assertEquals(NodeKind.Annotation, kind)
                    }
                }
            }
        }
    }

    @Test fun functionWithNoinlineParam() {
        verifyPackageMember("testdata/functions/functionWithNoinlineParam.kt", defaultModelConfig) { func ->
            with(func.details(NodeKind.Parameter).first()) {
                val modifiers = details(NodeKind.Modifier).map { it.name }
                assertTrue("noinline" in modifiers)
            }
        }
    }

    @Test fun annotatedFunctionWithAnnotationParameters() {
        checkSourceExistsAndVerifyModel(
            "testdata/functions/annotatedFunctionWithAnnotationParameters.kt",
            defaultModelConfig
        ) { model ->
            with(model.members.single().members.single { it.name == "f" }) {
                assertEquals(1, annotations.count())
                with(annotations[0]) {
                    assertEquals("Fancy", name)
                    assertEquals(Content.Empty, content)
                    assertEquals(NodeKind.Annotation, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(NodeKind.Parameter, kind)
                        assertEquals(1, details.count())
                        with(details[0]) {
                            assertEquals(NodeKind.Value, kind)
                            assertEquals("1", name)
                        }
                    }
                }
            }
        }
    }

    @Test fun functionWithDefaultParameter() {
        checkSourceExistsAndVerifyModel("testdata/functions/functionWithDefaultParameter.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                with(details.elementAt(3)) {
                    val value = details(NodeKind.Value)
                    assertEquals(1, value.count())
                    with(value[0]) {
                        assertEquals("\"\"", name)
                    }
                }
            }
        }
    }

    @Test fun sinceKotlin() {
        checkSourceExistsAndVerifyModel("testdata/functions/sinceKotlin.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("1.1", sinceKotlin)
            }
        }
    }
}

class JSFunctionTest: BaseFunctionTest(Platform.js)

class JVMFunctionTest: BaseFunctionTest(Platform.jvm) {
    @Test
    fun annotatedFunction() {
        verifyPackageMember("testdata/functions/annotatedFunction.kt", ModelConfig(
            analysisPlatform = Platform.jvm,
            withKotlinRuntime = true
        )) { func ->
            Assert.assertEquals(1, func.annotations.count())
            with(func.annotations[0]) {
                Assert.assertEquals("Strictfp", name)
                Assert.assertEquals(Content.Empty, content)
                Assert.assertEquals(NodeKind.Annotation, kind)
            }
        }
    }

}

class CommonFunctionTest: BaseFunctionTest(Platform.common)