/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.runnablesamples

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunnableSamplesInstallerTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    val writerPlugin = TestOutputWriterPlugin()

    @Test
    fun `should inject runnable-samples resources in html files`() {
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
                    assertTrue(it.value.contains(Regex("<link href=\"[./]*styles/runnable-samples.css\" rel=\"Stylesheet\">")))
                    assertTrue(it.value.contains(Regex("<script type=\"text/javascript\" src=\"[./]*scripts/runnable-samples.js\" async=\"async\"></script>")))
                }
            }
        }
    }

    @Test
    fun `should contain runnable-samples's resources`() {
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

                assertNotNull(contents["scripts/runnable-samples.js"])
                assertNotNull(contents["styles/runnable-samples.css"])
            }
        }
    }

    @Test
    fun `should override playgroundServer with custom configuration`() {
        val playgroundServer = "https://custom-playground-server.example.com"

        val customPlaygroundServerConfiguration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
            pluginsConfigurations.add(
                org.jetbrains.dokka.PluginConfigurationImpl(
                    RunnableSamplesPlugin.FQN,
                    org.jetbrains.dokka.DokkaConfiguration.SerializationFormat.JSON,
                    "{\"playgroundServer\": \"$playgroundServer\"}"
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
            configuration = customPlaygroundServerConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val scriptContent = writerPlugin.writer.contents["scripts/runnable-samples.js"]

                assertNotNull(scriptContent, "Script should be present")
                assertTrue(
                    scriptContent.contains("const playgroundServer = \"$playgroundServer\""),
                    "Custom playgroundServer should be injected into the script"
                )
                assertFalse(
                    scriptContent.contains("const playgroundServer = null"),
                    "Default playgroundServer value should be replaced"
                )
            }
        }
    }
}
