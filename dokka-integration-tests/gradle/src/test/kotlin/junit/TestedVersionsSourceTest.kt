/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit

import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.it.gradle.junit.KotlinBuiltInCompatibility.Incompatible
import org.jetbrains.dokka.it.gradle.junit.KotlinBuiltInCompatibility.Required
import org.junit.jupiter.api.Test

class TestedVersionsSourceTest {

    @Test
    fun `test default versions`() {
        val default = TestedVersionsSource.Default

        val versions = default.get()
            .map {
                it.run {
                    "gradle:$gradle - kgp:$kgp"
                }
            }
            .sorted()
            .joinToString("\n")

        versions.shouldBe(
            """
            gradle:7.6.4 - kgp:1.9.25
            gradle:7.6.4 - kgp:2.0.21
            gradle:8.14.3 - kgp:1.9.25
            gradle:8.14.3 - kgp:2.0.21
            gradle:9.1.0 - kgp:2.1.21
            gradle:9.1.0 - kgp:2.2.21
            gradle:9.1.0 - kgp:2.3.0-RC
            """.trimIndent()
        )
    }

    @Test
    fun `Android versions`() {
        val default = TestedVersionsSource.Android(
            kotlinBuiltIn = Required,
        )

        val versions = default.get()
            .map {
                it.run {
                    "gradle:$gradle - kgp:$kgp - agp:$agp"
                }
            }
            .sorted()
            .joinToString("\n")

        versions.shouldBe(
            """
            gradle:7.6.4 - kgp:1.9.25 - agp:7.4.2
            gradle:7.6.4 - kgp:2.0.21 - agp:7.4.2
            gradle:8.14.3 - kgp:1.9.25 - agp:8.11.2
            gradle:8.14.3 - kgp:1.9.25 - agp:8.12.3
            gradle:8.14.3 - kgp:1.9.25 - agp:8.13.0
            gradle:8.14.3 - kgp:2.0.21 - agp:8.11.2
            gradle:8.14.3 - kgp:2.0.21 - agp:8.12.3
            gradle:8.14.3 - kgp:2.0.21 - agp:8.13.0
            """.trimIndent()
        )
    }

    @Test
    fun `Android versions - with minimum`() {
        val default = TestedVersionsSource.Android(
            kotlinBuiltIn = Incompatible,
        )

        val versions = default.get()
            .map {
                it.run {
                    "gradle:$gradle - kgp:$kgp - agp:$agp"
                }
            }
            .sorted()
            .joinToString("\n")

        versions.shouldBe(
            """
            gradle:9.1.0 - kgp:2.1.21 - agp:9.0.0-beta01
            gradle:9.1.0 - kgp:2.2.21 - agp:9.0.0-beta01
            gradle:9.1.0 - kgp:2.3.0-RC - agp:9.0.0-beta01
            """.trimIndent()
        )
    }

    @Test
    fun `AndroidCompose versions`() {
        val default = TestedVersionsSource.AndroidCompose()

        val versions = default.get()
            .map {
                it.run {
                    "gradle:$gradle - kgp:$kgp - agp:$agp - compose:$composeGradlePlugin"
                }
            }
            .sorted()
            .joinToString("\n")

        versions.shouldBe(
            """
            gradle:7.6.4 - kgp:1.9.25 - agp:7.4.2 - compose:1.7.0
            gradle:7.6.4 - kgp:2.0.21 - agp:7.4.2 - compose:1.7.0
            gradle:8.14.3 - kgp:1.9.25 - agp:8.11.2 - compose:1.7.0
            gradle:8.14.3 - kgp:1.9.25 - agp:8.12.3 - compose:1.7.0
            gradle:8.14.3 - kgp:1.9.25 - agp:8.13.0 - compose:1.7.0
            gradle:8.14.3 - kgp:2.0.21 - agp:8.11.2 - compose:1.7.0
            gradle:8.14.3 - kgp:2.0.21 - agp:8.12.3 - compose:1.7.0
            gradle:8.14.3 - kgp:2.0.21 - agp:8.13.0 - compose:1.7.0
            gradle:9.1.0 - kgp:2.1.21 - agp:9.0.0-beta01 - compose:1.7.0
            gradle:9.1.0 - kgp:2.2.21 - agp:9.0.0-beta01 - compose:1.7.0
            gradle:9.1.0 - kgp:2.3.0-RC - agp:9.0.0-beta01 - compose:1.7.0
            """.trimIndent()
        )
    }

}
