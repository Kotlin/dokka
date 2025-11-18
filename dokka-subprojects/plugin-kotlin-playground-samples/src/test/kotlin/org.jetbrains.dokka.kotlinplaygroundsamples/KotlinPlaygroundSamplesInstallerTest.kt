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
    fun `should not inject playground resources when no samples are used`() {
        testInline(
            """
            |/src/main/kotlin/NoSample.kt
            |package com.example
            |
            | /**
            | * A simple class without any samples.
            | */
            |class Bar {
            |    fun regularFunction() {
            |        println("No sample here")
            |    }
            |}
            """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val contents = writerPlugin.writer.contents

                val allHtmlFiles = contents
                    .filter { (key, _) -> key.endsWith(".html") && key != "navigation.html" }

                allHtmlFiles.forEach { (path, content) ->
                    assertFalse(
                        content.contains(Regex("<link href=\"[./]*styles/kotlin-playground-samples.css\" rel=\"Stylesheet\">")),
                        "Page $path should not contain kotlin-playground-samples.css"
                    )
                    assertFalse(
                        content.contains(Regex("<script type=\"text/javascript\" src=\"[./]*scripts/kotlin-playground-samples.js\" async=\"async\"></script>")),
                        "Page $path should not contain kotlin-playground-samples.js"
                    )
                }
            }
        }
    }

    @Test
    fun `should inject kotlin-playground-samples resources only in html files with samples`() {
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
            | * Class with sample
            | * @sample [com.example.sampleFunction]
            | */
            |class Foo
            |
            | /**
            | * Class without sample
            | */
            |class Bar {
            |    fun regularFunction() {
            |        println("No sample here")
            |    }
            |}
            """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val contents = writerPlugin.writer.contents

                val allHtmlFiles = contents
                    .filter { (key, _) -> key.endsWith(".html") && key != "navigation.html" }

                val pageWithSample = allHtmlFiles.entries.find { (path, _) ->
                    path.endsWith("-foo/index.html")
                }
                assertNotNull(pageWithSample, "Page for Foo class should exist")

                assertTrue(
                    pageWithSample.value.contains(Regex("<link href=\"[./]*styles/kotlin-playground-samples.css\" rel=\"Stylesheet\">")),
                    "Page with sample should contain kotlin-playground-samples.css"
                )
                assertTrue(
                    pageWithSample.value.contains(Regex("<script type=\"text/javascript\" src=\"[./]*scripts/kotlin-playground-samples.js\" async=\"async\"></script>")),
                    "Page with sample should contain kotlin-playground-samples.js"
                )

                val pageWithoutSample = allHtmlFiles.entries.find { (path, _) ->
                    path.endsWith("-bar/index.html")
                }
                assertNotNull(pageWithoutSample, "Page for Bar class should exist")

                assertFalse(
                    pageWithoutSample.value.contains(Regex("<link href=\"[./]*styles/kotlin-playground-samples.css\" rel=\"Stylesheet\">")),
                    "Page without sample should NOT contain kotlin-playground-samples.css"
                )
                assertFalse(
                    pageWithoutSample.value.contains(Regex("<script type=\"text/javascript\" src=\"[./]*scripts/kotlin-playground-samples.js\" async=\"async\"></script>")),
                    "Page without sample should NOT contain kotlin-playground-samples.js"
                )
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
