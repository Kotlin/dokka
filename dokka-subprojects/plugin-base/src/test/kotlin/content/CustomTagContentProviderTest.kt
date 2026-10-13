/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content

import matchers.content.assertNode
import matchers.content.check
import matchers.content.group
import matchers.content.header
import matchers.content.link
import matchers.content.platformHinted
import matchers.content.skipAllNotMatching
import matchers.content.table
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomTagContentProviderTest : BaseAbstractTest() {

    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `custom tag content provider brief is present for constructors`(){
        testInline(
            """
            |/src/main/kotlin/com/example/foo.kt
            |package com.example
            |
            |class Foo {
            |/**
            | * @customTag custom tag for constructor
            | */
            | constructor()
            |}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(TestPluginWithCustomTagContentProvider())
        ) {
            renderingStage = { rootPageNode, _ ->
                val page = rootPageNode.children
                    .flatMap { it.children }
                    .filterIsInstance<ContentPage>()
                    .single { it.name == "Foo" }

                page.content.assertNode {
                    skipAllNotMatching()
                    group {
                        group {
                            group {
                                header { +"Constructors" }
                                table {
                                    group {
                                        link { +"Foo" }
                                        platformHinted {
                                            group {
                                                skipAllNotMatching()
                                            }
                                            group {
                                                check {
                                                    assertEquals(dci.kind, ContentKind.BriefComment)
                                                }
                                                skipAllNotMatching()
                                            }
                                            group {
                                                group {
                                                    +"custom tag for constructor"
                                                }
                                            }
                                            skipAllNotMatching()
                                        }
                                        skipAllNotMatching()
                                    }
                                }
                                skipAllNotMatching()
                            }
                        }
                        skipAllNotMatching()
                    }
                }
            }
        }
    }

    @Test
    fun `custom tag content provider brief is present in Entries table for enums`() {
        testInline(
            """
            |/src/main/kotlin/com/example/foo.kt
            |package com.example
            |
            |enum class Foo {
            |/**
            | * @customTag custom tag for a
            | */
            | A 
            |}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(TestPluginWithCustomTagContentProvider())
        ) {
            renderingStage = { rootPageNode, _ ->
                val page = rootPageNode.children
                    .flatMap { it.children }
                    .filterIsInstance<ContentPage>()
                    .single { it.name == "Foo" }

                page.content.assertNode {
                    skipAllNotMatching()
                    group {
                        group {
                            group {
                                header { +"Entries" }
                                table {
                                    group {
                                        link { +"A" }
                                        platformHinted {
                                            group {
                                                skipAllNotMatching()
                                            }
                                            group {
                                                check {
                                                    assertEquals(dci.kind, ContentKind.BriefComment)
                                                }
                                                skipAllNotMatching()
                                            }
                                            group {
                                                group {
                                                    +"custom tag for a"
                                                }
                                            }
                                            skipAllNotMatching()
                                        }
                                        skipAllNotMatching()
                                    }
                                }
                                skipAllNotMatching()
                            }
                        }
                        skipAllNotMatching()
                    }
                }
            }
        }
    }
}

class TestPluginWithCustomTagContentProvider : DokkaPlugin() {
    @Suppress("unused") // This delegated property has a desired side effect.
    val customCodeContentProvider: Extension<CustomTagContentProvider, *, *> by extending {
        plugin<DokkaBase>().customTagContentProvider providing {
            TestCustomTagContentProvider()
        }
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}

class TestCustomTagContentProvider : CustomTagContentProvider {
    override fun isApplicable(customTag: CustomTagWrapper): Boolean {
        return customTag.name == "customTag"
    }

    override fun PageContentBuilder.DocumentableContentBuilder.contentForBrief(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        comment(customTag.children.single())
    }
}