/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import utils.AbstractModelTest
import utils.OnlyDescriptors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "types") {

    @Test
    fun `type with typealias to functional type with parameter`() {
        inlineModelTest(
            """
            |typealias HttpExceptionCallback<T> = String.(T) -> String
            |fun <T> exception(callback: HttpExceptionCallback<T>){}"""
        ) {
            with((this / "types" / "HttpExceptionCallback").cast<DTypeAlias>()) {
                name equals "HttpExceptionCallback"
                assertTrue(type is GenericTypeConstructor)
                (type as GenericTypeConstructor).projections counts 1
                underlyingType.values.first().driOrNull equals DRI("kotlin", "Function2")
            }
            with((this / "types" / "exception").cast<DFunction>()) {
                name equals "exception"
                val parameterType = parameters.first().type
                assertTrue(parameterType is TypeAliased)
                with(parameterType) {
                    assertTrue(typeAlias is GenericTypeConstructor)
                    (typeAlias as GenericTypeConstructor).projections counts 1
                    assertTrue(inner is FunctionalTypeConstructor)
                    (inner as FunctionalTypeConstructor).dri equals DRI("kotlin", "Function2")
                    (inner as FunctionalTypeConstructor).projections counts 3
                }
            }
        }
    }

    @Test
    fun `type with typealias to functional type`() {
        inlineModelTest(
            """
            |typealias CompletionHandler = (cause: Throwable?) -> Unit
            |fun exception(callback: CompletionHandler){}"""
        ) {
            with((this / "types" / "CompletionHandler").cast<DTypeAlias>()) {
                assertTrue(type is GenericTypeConstructor)
                (type as GenericTypeConstructor).projections counts 0
                name equals "CompletionHandler"
                underlyingType.values.first().driOrNull equals DRI("kotlin", "Function1")
            }
            with((this / "types" / "exception").cast<DFunction>()) {
                name equals "exception"
                val parameterType = parameters.first().type
                assertTrue(parameterType is TypeAliased)
                with(parameterType) {
                    assertTrue(typeAlias is GenericTypeConstructor)
                    assertTrue(inner is FunctionalTypeConstructor)
                    (inner as FunctionalTypeConstructor).dri equals DRI("kotlin", "Function1")
                    (inner as FunctionalTypeConstructor).projections counts 2
                }
            }
        }
    }

    @Test
    fun `function types with a parameter name should have implicit @ParameterName annotations with mustBeDocumented=false`() {
        inlineModelTest(
            """
            |val nF: (param: Int) -> String = { _ -> "" }"""
        ) {
            with((this / "types" / "nF").cast<DProperty>()) {
                assertTrue(type is FunctionalTypeConstructor)
                val parameterType =
                    ((type as FunctionalTypeConstructor).projections[0] as Invariance<*>).inner as GenericTypeConstructor
                val annotation = parameterType.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                assertEquals(
                    Annotations.Annotation(
                        dri = DRI("kotlin", "ParameterName"),
                        params = mapOf("name" to StringValue("param")),
                        mustBeDocumented = false
                    ),
                    annotation
                )
            }
        }
    }

    @Test
    fun `explicit @ParameterName annotations should have mustBeDocumented=true`() {
        inlineModelTest(
            """
            |val nF:  (@ParameterName(name="param") Int) -> String = { _ -> "" }"""
        ) {
            with((this / "types" / "nF").cast<DProperty>()) {
                assertTrue(type is FunctionalTypeConstructor)
                val parameterType =
                    ((type as FunctionalTypeConstructor).projections[0] as Invariance<*>).inner as GenericTypeConstructor
                val annotation = parameterType.extra[Annotations]?.directAnnotations?.values?.single()?.single()
                assertEquals(
                    Annotations.Annotation(
                        dri = DRI("kotlin", "ParameterName"),
                        params = mapOf("name" to StringValue("param")),
                        mustBeDocumented = true
                    ),
                    annotation
                )
            }
        }
    }
}
