/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.documentable

import org.jetbrains.dokka.analysis.test.OnlyDescriptors
import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.ObviousMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ObviousFunctionsTest {

    @OnlyDescriptors("#3354")
    @Test
    fun `kotlin_Any should not have obvious members`() {
        val project = kotlinJvmTestProject {
            ktFile("kotlin/Any.kt") {
                +"""
                    package kotlin
                    public open class Any {
                        public open fun equals(other: Any?): Boolean
                        public open fun hashCode(): Int
                        public open fun toString(): String
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val any = pkg.classlikes.single()

        assertEquals("Any", any.name)

        assertObviousFunctions(
            expectedObviousFunctions = emptySet(),
            expectedNonObviousFunctions = setOf("equals", "hashCode", "toString"),
            actualFunctions = any.functions
        )
    }

    @Test
    fun `kotlin_Any should not have obvious members via external documentable provider`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"class SomeClass"
            }
        }

        project.useServices {
            val any = externalDocumentableProvider.getClasslike(
                DRI("kotlin", "Any"),
                it.context.configuration.sourceSets.single()
            )
            assertNotNull(any)
            assertEquals("Any", any.name)

            assertObviousFunctions(
                expectedObviousFunctions = emptySet(),
                expectedNonObviousFunctions = setOf("equals", "hashCode", "toString"),
                actualFunctions = any.functions
            )
        }
    }

    // when running with K2 - inherited from java enum functions available: "clone", "finalize", "getDeclaringClass"
    @OnlyDescriptors("#3196")
    @Test
    fun `kotlin_Enum should not have obvious members`() {
        val project = kotlinJvmTestProject {
            ktFile("kotlin/Any.kt") {
                +"""
                    package kotlin
                    public abstract class Enum<E : Enum<E>>(name: String, ordinal: Int): Comparable<E> {
                        public override final fun compareTo(other: E): Int
                        public override final fun equals(other: Any?): Boolean
                        public override final fun hashCode(): Int
                        public override fun toString(): String
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val any = pkg.classlikes.single()

        assertEquals("Enum", any.name)

        assertObviousFunctions(
            expectedObviousFunctions = emptySet(),
            expectedNonObviousFunctions = setOf("compareTo", "equals", "hashCode", "toString"),
            actualFunctions = any.functions
        )
    }

    // when running with K2 there is no equals, hashCode, toString present
    @OnlyDescriptors("#3196")
    @Test
    fun `kotlin_Enum should not have obvious members via external documentable provider`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"class SomeClass"
            }
        }

        project.useServices {
            val enum = externalDocumentableProvider.getClasslike(
                DRI("kotlin", "Enum"),
                it.context.configuration.sourceSets.single()
            )
            assertNotNull(enum)
            assertEquals("Enum", enum.name)

            assertObviousFunctions(
                expectedObviousFunctions = emptySet(),
                expectedNonObviousFunctions = setOf(
                    "compareTo", "equals", "hashCode", "toString",
                    // inherited from java enum
                    "clone", "finalize", "getDeclaringClass"
                ),
                actualFunctions = enum.functions
            )
        }
    }

    private fun assertObviousFunctions(
        expectedObviousFunctions: Set<String>,
        expectedNonObviousFunctions: Set<String>,
        actualFunctions: List<DFunction>
    ) {
        val (notObviousFunctions, obviousFunctions) = actualFunctions.partition {
            it.extra[ObviousMember] == null
        }

        assertEquals(
            expectedNonObviousFunctions,
            notObviousFunctions.map { it.name }.toSet()
        )

        assertEquals(
            expectedObviousFunctions,
            obviousFunctions.map { it.name }.toSet()
        )
    }

}
