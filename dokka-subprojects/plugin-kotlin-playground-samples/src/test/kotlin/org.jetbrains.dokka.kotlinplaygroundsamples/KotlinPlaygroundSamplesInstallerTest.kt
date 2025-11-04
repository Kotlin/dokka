/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplaygroundsamples

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinPlaygroundSamplesInstallerTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    val writerPlugin = TestOutputWriterPlugin()

    @Test
    fun `should inject kotlin-playground-samples resources in html files`() {
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
            configuration = configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val contents = writerPlugin.writer.contents

                val allHtmlFiles = contents
                    .filter { (key, _) -> key.endsWith(".html") && key != "navigation.html" }

                allHtmlFiles.forEach {
                    assertTrue(it.value.contains(Regex("<link href=\"[./]*styles/kotlin-playground-samples.css\" rel=\"Stylesheet\">")))
                    assertTrue(it.value.contains(Regex("<script type=\"text/javascript\" src=\"[./]*scripts/kotlin-playground-samples.js\" async=\"async\"></script>")))
                }
            }
        }
    }

    @Test
    fun `should contain kotlin-playground-samples's resources`() {
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
            configuration = configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val contents = writerPlugin.writer.contents

                assertNotNull(contents["scripts/kotlin-playground-samples.js"])
                assertNotNull(contents["styles/kotlin-playground-samples.css"])
            }
        }
    }

    @Test
    fun `should override kotlinPlaygroundServer with custom configuration`() {
        val kotlinPlaygroundServer = "https://custom-playground-server.example.com"

        val customKotlinPlaygroundServerConfiguration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
            pluginsConfigurations.add(
                org.jetbrains.dokka.PluginConfigurationImpl(
                    KotlinPlaygroundSamplesPlugin.FQN,
                    org.jetbrains.dokka.DokkaConfiguration.SerializationFormat.JSON,
                    "{\"kotlinPlaygroundServer\": \"$kotlinPlaygroundServer\"}"
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
            configuration = customKotlinPlaygroundServerConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val scriptContent = writerPlugin.writer.contents["scripts/kotlin-playground-samples.js"]

                assertNotNull(scriptContent, "Script should be present")
                assertTrue(
                    scriptContent.contains("const kotlinPlaygroundServer = \"$kotlinPlaygroundServer\""),
                    "Custom kotlinPlaygroundServer should be injected into the script"
                )
                assertFalse(
                    scriptContent.contains("const kotlinPlaygroundServer = null"),
                    "Default kotlinPlaygroundServer value should be replaced"
                )
            }
        }
    }
}
