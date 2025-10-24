/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.runnablesamples

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.plugability.plugin
import kotlin.test.Test
import kotlin.test.assertNotNull

class RunnableSamplesPluginTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `RunnableSamplesPlugin is loaded into DokkaContext`() {
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
                val runnableSamplesPlugin = context.plugin<RunnableSamplesPlugin>()
                assertNotNull(runnableSamplesPlugin)
            }
        }
    }
}
