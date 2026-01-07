/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit

import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.it.gradle.junit.KotlinBuiltInCompatibility.*
import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.toMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TestedVersionsSourceTest {

    @Test
    fun `test default versions`() {
        val actual = TestedVersionsSource.Default.get().joinAllToString()

        actual shouldBe """
            gradle: 7.6.4, 8.14.3, 9.1.0
            kgp: 1.9.25, 2.0.21, 2.1.21, 2.2.21, 2.3.0-RC
        """.trimIndent()
    }

    @ParameterizedTest
    @EnumSource(KotlinBuiltInCompatibility::class)
    fun `Android versions`(
        kotlinBuiltIn: KotlinBuiltInCompatibility
    ) {
        val versions = TestedVersionsSource.Android(kotlinBuiltIn = kotlinBuiltIn)
            .get().joinAllToString()

        val expected = when (kotlinBuiltIn) {
            Required ->
                """
                agp: 9.0.0-beta01
                gradle: 9.1.0
                kgp: 2.1.21, 2.2.21, 2.3.0-RC
                """.trimIndent()

            Supported ->
                """
                agp: 7.4.2, 8.11.2, 8.12.3, 8.13.0, 9.0.0-beta01
                gradle: 7.6.4, 8.14.3, 9.1.0
                kgp: 1.9.25, 2.0.21, 2.1.21, 2.2.21, 2.3.0-RC
                """.trimIndent()

            Incompatible ->
                """
                agp: 7.4.2, 8.11.2, 8.12.3, 8.13.0
                gradle: 7.6.4, 8.14.3
                kgp: 1.9.25, 2.0.21
                """.trimIndent()
        }

        versions shouldBe expected
    }

    @ParameterizedTest
    @EnumSource(KotlinBuiltInCompatibility::class)
    fun `AndroidCompose versions`(
        kotlinBuiltIn: KotlinBuiltInCompatibility
    ) {
        val versions = TestedVersionsSource.AndroidCompose(kotlinBuiltIn = kotlinBuiltIn)
            .get().joinAllToString()

        val expected = when (kotlinBuiltIn) {
            Required ->
                """
                agp: 9.0.0-beta01
                composeGradlePlugin: 1.7.0
                gradle: 9.1.0
                kgp: 2.1.21, 2.2.21, 2.3.0-RC
                """.trimIndent()

            Supported ->
                """
                agp: 7.4.2, 8.11.2, 8.12.3, 8.13.0, 9.0.0-beta01
                composeGradlePlugin: 1.7.0
                gradle: 7.6.4, 8.14.3, 9.1.0
                kgp: 1.9.25, 2.0.21, 2.1.21, 2.2.21, 2.3.0-RC
                """.trimIndent()

            Incompatible ->
                """
                agp: 7.4.2, 8.11.2, 8.12.3, 8.13.0
                composeGradlePlugin: 1.7.0
                gradle: 7.6.4, 8.14.3
                kgp: 1.9.25, 2.0.21
                """.trimIndent()
        }

        versions shouldBe expected
    }

    companion object {
        private fun Sequence<TestedVersions>.joinAllToString(
            // DGP version is set via system prop and changes - see `org.jetbrains.dokka.integration_test.dokkaVersionOverride`.
            // Ignore it, because the DGP version isn't relevant here.
            exclude: Set<String> = setOf("dgp")
        ): String =
            flatMap { it.toMap().entries }
                .groupingBy { it.key }
                .fold(emptySet<String>()) { accumulator, element ->
                    accumulator + element.value.toString()
                }
                .entries
                .filter { it.key !in exclude }
                .sortedBy { it.key }
                .joinToString("\n") { (k, v) ->
                    "${k}: ${v.joinToString()}"
                }
    }
}
