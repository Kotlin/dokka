/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.UnresolvedBound
import org.jetbrains.dokka.model.UnresolvedTypeExtra
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SymbolsTranslatorTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `should warn about unresolved symbol`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |fun f(a: UnresolvedSymbols)
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warn = logger.warnMessages.first()
                val path = m.sourceSets.first().sourceRoots.first().invariantSeparatorsPath

                assertEquals(
                    "`UnresolvedSymbols` is unresolved in file:///PATH/Test.kt:1:7",
                    warn.replace(path, "PATH")
                )
            }
        }
    }

    @Test
    fun `should warn about unresolved context parameters of documented functions and properties`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |abstract class Base {
            |    context(logger: FunctionLogger)
            |    fun functionWithContext() {}
            |
            |    context(logger: PropertyLogger, other: List<NestedLogger>)
            |    val propertyWithContext: Int get() = 0
            |
            |    context(logger: PrivateLogger)
            |    private val privateProperty: Int get() = 0
            |
            |    context(logger: String)
            |    val resolvedProperty: Int get() = 0
            |}
            |
            |abstract class Derived : Base()
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = {
                val warnings = logger.warnMessages
                assertEquals(3, warnings.size, warnings.joinToString())
                for (name in listOf("FunctionLogger", "PropertyLogger", "NestedLogger")) {
                    assertEquals(1, warnings.count { it.startsWith("`$name` is unresolved in ") })
                }
            }
        }
    }

    @Test
    fun `should add UnresolvedTypeExtra to unresolved type`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |fun f(a: UnresolvedSymbols)
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val path = module.sourceSets.single().sourceRoots.first().invariantSeparatorsPath
                val parameterType = module.packages.single()
                    .functions.single()
                    .parameters.single()
                    .type as? UnresolvedBound

                assertNotNull(parameterType)
                val extra = parameterType.extra[UnresolvedTypeExtra]

                assertNotNull(extra)
                assertEquals("`UnresolvedSymbols` is unresolved", extra.message)
                assertEquals(
                    "file:///PATH/Test.kt:1:7",
                    extra.location.replace(path, "PATH"),
                )
            }
        }
    }

    @Test
    fun `should warn about unresolved annotation`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |fun f(a: @Unresolved(1) Int) = 0
            |
            |@Deprecated(asda)
            |fun f(a: Int) = 0
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warns = logger.warnMessages
                val path = m.sourceSets.first().sourceRoots.first().invariantSeparatorsPath

                assertEquals(
                    "Unknown annotation `@Unresolved(1)` in file:///PATH/Test.kt:1:10",
                    warns[0].replace(path, "PATH")
                )
                assertEquals(
                    "Unsupported annotation value `@Deprecated(asda)` in file:///PATH/Test.kt:3:1",
                    warns[1].replace(path, "PATH")
                )
                assertEquals(
                    1,
                    logger.debugMessages.size
                )
            }
        }
    }

    @Test
    fun `should warn about multiline unresolved annotation on a single line`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |@Unresolved(
            |    value = 0
            |)
            |class Foo
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { m ->
                val warns = logger.warnMessages
                val path = m.sourceSets.first().sourceRoots.first().invariantSeparatorsPath

                assertEquals(
                    "Unknown annotation `@Unresolved( value = 0 )` in file:///PATH/Test.kt:1:1",
                    warns[0].replace(path, "PATH")
                )
            }
        }
    }
}
