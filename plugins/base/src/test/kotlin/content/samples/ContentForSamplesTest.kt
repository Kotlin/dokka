/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.samples

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.pages.KOTLIN_PLAYGROUND_SCRIPT
import org.jetbrains.dokka.model.DisplaySourceSet
import utils.TestOutputWriterPlugin
import utils.assertContains
import utils.classSignature
import utils.findTestType
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ContentForSamplesTest : BaseAbstractTest() {
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

    private val mppTestConfiguration = dokkaConfiguration {
        moduleName = "example"
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                samples = listOf(
                    Paths.get("$testDataDir/samples.kt").toString(),
                )
            }
            sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                samples = listOf(
                    Paths.get("$testDataDir/samples.kt").toString(),
                )
            }
            sourceSet {
                name = "linuxX64"
                displayName = "linuxX64"
                analysisPlatform = "native"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                samples = listOf(
                    Paths.get("$testDataDir/samples.kt").toString(),
                )
            }
        }
    }

    @Test
    fun `samples block is rendered in the description`() {
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                assertContains(page.embeddedResources, KOTLIN_PLAYGROUND_SCRIPT)
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
                assertNotEquals(-1, writerPlugin.writer.contents["root/test/-foo/index.html"]?.indexOf(KOTLIN_PLAYGROUND_SCRIPT))
            }
        }
    }

    @Test
    fun `multiplatofrm class with samples in few platforms`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @sample [test.sampleForClassDescription]
                |*/
                |expect open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @sample unresolved
                |*/
                |actual open class Parent
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual open class Parent
                |
            """.trimMargin(),
            mppTestConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "Parent")
                assertContains(page.embeddedResources, KOTLIN_PLAYGROUND_SCRIPT)
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            group {
                                +"expect open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
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
                                check {
                                    sourceSets.assertSourceSet("common")
                                }
                            }
                            group {
                                +"unresolved"
                                check {
                                    sourceSets.assertSourceSet("jvm")
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }
}


private fun Set<DisplaySourceSet>.assertSourceSet(expectedName: String) {
    assertEquals(1, this.size)
    assertEquals(expectedName, this.first().name)
}
