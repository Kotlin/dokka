/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinPlaygroundSamples

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.MemberPageNode
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
                val pageNodes = root.children.first().children
                val classFooResources = (pageNodes[0] as ClasslikePageNode).embeddedResources
                val sampleFunctionResources = (pageNodes[1] as MemberPageNode).embeddedResources

                assertFalse("Page without sample should not contain kotlin playground script") {
                    sampleFunctionResources.contains(defaultScript)
                }

                assertTrue("Page with sample contain default kotlin playground script") {
                    classFooResources.contains(defaultScript)
                }
            }
        }
    }

    @Test
    fun `should contain custom kotlin playground script`() {
        val customScript = "customKotlinPlaygroundScript.js"
        val customKotlinPlaygroundScriptConfiguration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
            pluginsConfigurations.add(
                PluginConfigurationImpl(
                    KotlinPlaygroundSamplesPlugin::class.qualifiedName!!,
                    DokkaConfiguration.SerializationFormat.JSON,
                    "{\"kotlinPlaygroundScript\": \"$customScript\"}"
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
                val pageNodes = root.children.first().children
                val classFooResources = (pageNodes[0] as ClasslikePageNode).embeddedResources
                val sampleFunctionResources = (pageNodes[1] as MemberPageNode).embeddedResources

                assertFalse("Page without sample should not contain kotlin playground script") {
                    sampleFunctionResources.contains(defaultScript) || sampleFunctionResources.contains(customScript)
                }

                assertFalse("When custom kotlin playground script is included, page shouldn't contain default kotlin playground script") {
                    classFooResources.contains(defaultScript)
                }

                assertTrue("Should contain custom kotlin playground script") {
                    classFooResources.contains(customScript)
                }
            }
        }
    }
}
