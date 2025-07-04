/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplayground

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import utils.assertContains
import utils.classSignature
import utils.findTestType
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

class KotlinPlaygroundPluginTest : BaseAbstractTest() {
    private val testDataDir = getTestDataDir("content/samples").toAbsolutePath()

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                samples = listOf(
                    Paths.get("$testDataDir/samples.kt").toString(),
                )
            }
        }
    }

    @Test
    fun `samples are made runnable when KotlinPlaygroundPlugin is enabled`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            | /**
            | * @sample [test.sampleForClassDescription]
            | */
            |class Foo
        """.trimIndent(), testConfiguration,
            pluginOverrides = listOf(writerPlugin, KotlinPlaygroundPlugin())
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                // Should contain the default playground script
                assertContains(page.embeddedResources, DEFAULT_KOTLIN_PLAYGROUND_SCRIPT)
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo"
                            )
                            header(4) { +"Samples" }
                            group {
                                codeBlock {
                                    +"""|
                                    |fun main() { 
                                    |   //sampleStart 
                                    |   print("Hello") 
                                    |   //sampleEnd
                                    |}""".trimMargin()
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
            renderingStage = { _, _ ->
                // Should contain playground script in the output
                assertTrue(writerPlugin.writer.contents["root/test/-foo/index.html"]?.contains("playground") ?: false)
            }
        }
    }
}