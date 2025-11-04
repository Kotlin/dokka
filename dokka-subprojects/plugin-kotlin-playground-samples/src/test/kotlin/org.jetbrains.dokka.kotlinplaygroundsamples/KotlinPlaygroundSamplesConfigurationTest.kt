/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplaygroundsamples

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.ContentPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinPlaygroundSamplesConfigurationTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    private val defaultScript = KotlinPlaygroundSamplesConfiguration.defaultKotlinPlaygroundScript

    @Test
    fun `default kotlinPlaygroundScript is correct`() {
        assertEquals(
            "https://unpkg.com/kotlin-playground@1/dist/playground.min.js",
            defaultScript
        )
    }

    @Test
    fun `should contain default kotlin playground script`() {
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
            pagesTransformationStage = { root ->
                val contentPages = root.children.filterIsInstance<ContentPage>()
                val defaultScriptIncluded = contentPages.all {
                    defaultScript in it.embeddedResources
                }
                assertTrue(defaultScriptIncluded, "Default Kotlin Playground script should be included")
            }
        }
    }

    @Test
    fun `should contain custom kotlin playground script`() {
        val customKotlinPlaygroundScriptConfiguration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
            pluginsConfigurations.add(
                PluginConfigurationImpl(
                    KotlinPlaygroundSamplesPlugin.FQN,
                    DokkaConfiguration.SerializationFormat.JSON,
                    "{\"kotlinPlaygroundScript\": \"customKotlinPlaygroundScript.js\"}"
                )
            )
        }

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
            configuration = customKotlinPlaygroundScriptConfiguration
        ) {
            pagesTransformationStage = { root ->
                val contentPages = root.children.filterIsInstance<ContentPage>()
                val defaultScriptIncluded = contentPages.all {
                    defaultScript in it.embeddedResources
                }

                assertFalse(
                    defaultScriptIncluded,
                    "When custom kotlin playground script is included, page shouldn't contain default kotlin playground script"
                )

                val customScriptIncluded = contentPages.all {
                    "customKotlinPlaygroundScript.js" in it.embeddedResources
                }
                assertTrue(customScriptIncluded, "Should contain custom kotlin playground script")
            }
        }
    }
}
