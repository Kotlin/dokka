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
        val default = TestedVersionsSource.Default

        val versions = default.get().joinAllToString()

        versions.shouldBe(
            """
            {gradle=7.6.4, kgp=1.9.25}
            {gradle=8.14.3, kgp=1.9.25}
            {gradle=7.6.4, kgp=2.0.21}
            {gradle=8.14.3, kgp=2.0.21}
            {gradle=9.1.0, kgp=2.1.21}
            {gradle=9.1.0, kgp=2.2.21}
            {gradle=9.1.0, kgp=2.3.0-RC}
            """.trimIndent()
        )
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
                {gradle=9.1.0, kgp=2.1.21, agp=9.0.0-beta01}
                {gradle=9.1.0, kgp=2.2.21, agp=9.0.0-beta01}
                {gradle=9.1.0, kgp=2.3.0-RC, agp=9.0.0-beta01}
                """.trimIndent()

            Supported ->
                """
                {gradle=7.6.4, kgp=1.9.25, agp=7.4.2}
                {gradle=8.14.3, kgp=1.9.25, agp=8.11.2}
                {gradle=8.14.3, kgp=1.9.25, agp=8.12.3}
                {gradle=8.14.3, kgp=1.9.25, agp=8.13.0}
                {gradle=7.6.4, kgp=2.0.21, agp=7.4.2}
                {gradle=8.14.3, kgp=2.0.21, agp=8.11.2}
                {gradle=8.14.3, kgp=2.0.21, agp=8.12.3}
                {gradle=8.14.3, kgp=2.0.21, agp=8.13.0}
                {gradle=9.1.0, kgp=2.1.21, agp=9.0.0-beta01}
                {gradle=9.1.0, kgp=2.2.21, agp=9.0.0-beta01}
                {gradle=9.1.0, kgp=2.3.0-RC, agp=9.0.0-beta01}
                """.trimIndent()

            Incompatible ->
                """
                {gradle=7.6.4, kgp=1.9.25, agp=7.4.2}
                {gradle=8.14.3, kgp=1.9.25, agp=8.11.2}
                {gradle=8.14.3, kgp=1.9.25, agp=8.12.3}
                {gradle=8.14.3, kgp=1.9.25, agp=8.13.0}
                {gradle=7.6.4, kgp=2.0.21, agp=7.4.2}
                {gradle=8.14.3, kgp=2.0.21, agp=8.11.2}
                {gradle=8.14.3, kgp=2.0.21, agp=8.12.3}
                {gradle=8.14.3, kgp=2.0.21, agp=8.13.0}
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
                {gradle=9.1.0, kgp=2.1.21, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                {gradle=9.1.0, kgp=2.2.21, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                {gradle=9.1.0, kgp=2.3.0-RC, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                """.trimIndent()

            Supported ->
                """
                {gradle=7.6.4, kgp=1.9.25, agp=7.4.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.11.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.12.3, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.13.0, composeGradlePlugin=1.7.0}
                {gradle=7.6.4, kgp=2.0.21, agp=7.4.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.11.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.12.3, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.13.0, composeGradlePlugin=1.7.0}
                {gradle=9.1.0, kgp=2.1.21, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                {gradle=9.1.0, kgp=2.2.21, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                {gradle=9.1.0, kgp=2.3.0-RC, agp=9.0.0-beta01, composeGradlePlugin=1.7.0}
                """.trimIndent()

            Incompatible ->
                """
                {gradle=7.6.4, kgp=1.9.25, agp=7.4.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.11.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.12.3, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=1.9.25, agp=8.13.0, composeGradlePlugin=1.7.0}
                {gradle=7.6.4, kgp=2.0.21, agp=7.4.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.11.2, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.12.3, composeGradlePlugin=1.7.0}
                {gradle=8.14.3, kgp=2.0.21, agp=8.13.0, composeGradlePlugin=1.7.0}
                """.trimIndent()
        }

        versions shouldBe expected
    }

    companion object {
        private fun Sequence<TestedVersions>.joinAllToString(): String =
            joinToString("\n") { versions ->
                versions.toMap()
                    .filterKeys { it != "dgp" }
                    .toString()
            }
    }

}
