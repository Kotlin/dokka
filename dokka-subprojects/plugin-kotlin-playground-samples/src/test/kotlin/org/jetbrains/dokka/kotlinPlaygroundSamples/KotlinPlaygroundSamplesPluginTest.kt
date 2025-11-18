/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinPlaygroundSamples

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.plugability.plugin
import kotlin.test.Test
import kotlin.test.assertNotNull

class KotlinPlaygroundSamplesPluginTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `KotlinPlaygroundSamplesPlugin is loaded into DokkaContext`() {
        testInline(
            """
            |/src/main/kotlin/Sample.kt
            |package com.example
            |
            |fun sampleFunction() {
            |    println("This is a sample")
            |}
            |
            | /**
            | * @sample [com.example.sampleFunction]
            | */
            |class Foo
            """.trimMargin(),
            configuration = configuration
        ) {
            pluginsSetupStage = { context ->
                val kotlinPlaygroundSamplesPlugin = context.plugin<KotlinPlaygroundSamplesPlugin>()
                assertNotNull(kotlinPlaygroundSamplesPlugin)
            }
        }
    }
}
