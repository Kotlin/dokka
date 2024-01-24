/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package resourceLinks

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.html.TEMPLATE_REPLACEMENT
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import utils.TestOutputWriterPlugin
import utils.assertContains
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceLinksTest : BaseAbstractTest() {
    class TestResourcesAppenderPlugin(val resources: List<String>) : DokkaPlugin() {
        class TestResourcesAppender(val resources: List<String>) : PageTransformer {
            override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
                it.modified(
                    embeddedResources = it.embeddedResources + resources
                )
            }
        }

        val appender by extending {
            plugin<DokkaBase>().htmlPreprocessors with TestResourcesAppender(resources)
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    @Test
    fun resourceLinksTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val absoluteResources = listOf(
            "https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css",
            "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js"
        )
        val relativeResources = listOf(
            "test/relativePath.js",
            "test/relativePath.css"
        )

        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(TestResourcesAppenderPlugin(absoluteResources + relativeResources), writerPlugin)
        ) {
            renderingStage = { _, _ ->
                Jsoup
                    .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                    .head()
                    .select("link, script")
                    .let {
                        absoluteResources.forEach { r ->
                            assertTrue(it.`is`("[href=$r], [src=$r]"))
                        }
                        relativeResources.forEach { r ->
                            assertTrue(it.`is`("[href=../$r] , [src=../$r]"))
                        }
                    }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun resourceCustomPreprocessorTest(isMultiModule: Boolean) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
            delayTemplateSubstitution = isMultiModule
            pluginsConfigurations = mutableListOf(
                PluginConfigurationImpl(
                    DokkaBase::class.java.canonicalName,
                    DokkaConfiguration.SerializationFormat.JSON,
                    toJsonString(
                        DokkaBaseConfiguration(
                            customStyleSheets = listOf(File("test/customStyle.css")),
                            customAssets = listOf(File("test/customImage.svg"))
                        )
                    )
                )
            )
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                run {
                    if (isMultiModule) {
                        assertNull(writerPlugin.writer.contents["images/customImage.svg"])
                        assertNull(writerPlugin.writer.contents["styles/customStyle.css"])
                    } else {
                        assertNotNull(writerPlugin.writer.contents["images/customImage.svg"])
                        assertNotNull(writerPlugin.writer.contents["styles/customStyle.css"])
                    }
                    if (isMultiModule) {
                        Jsoup
                            .parse(writerPlugin.writer.contents.getValue("example.html"))
                            .head()
                            .select("link, script")
                            .let {
                                listOf("styles/customStyle.css").forEach { r ->
                                    assertTrue(it.`is`("[href=$TEMPLATE_REPLACEMENT$r]"))
                                }
                            }
                    } else {
                        Jsoup
                            .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                            .head()
                            .select("link, script")
                            .let {
                                listOf("styles/customStyle.css").forEach { r ->
                                    assertTrue(it.`is`("[href=../$r], [src=../$r]"))
                                }
                            }
                    }
                }
            }
        }
    }

    @Test
    fun resourceMultiModuleLinksTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
            delayTemplateSubstitution = false
        }
        val absoluteResources = listOf(
            "https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css",
            "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js"
        )
        val relativeResources = listOf(
            "test/relativePath.js",
            "test/relativePath.css"
        )

        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(TestResourcesAppenderPlugin(absoluteResources + relativeResources), writerPlugin)
        ) {
            renderingStage = { _, _ ->
                run {
                    assertNull(writerPlugin.writer.contents["scripts/relativePath.js"])
                    assertNull(writerPlugin.writer.contents["styles/relativePath.js"])
                    Jsoup
                        .parse(writerPlugin.writer.contents.getValue("root/example.html"))
                        .head()
                        .select("link, script")
                        .let {
                            absoluteResources.forEach { r ->
                                assertTrue(it.`is`("[href=$r], [src=$r]"))
                            }
                            relativeResources.forEach { r ->
                                assertTrue(it.`is`("[href=../$r] , [src=../$r]"))
                            }
                        }
                }
            }
        }
    }

    @Test // see #3040; plain text added to <head> can be rendered by engines inside <body> as well
    fun `should not add unknown resources as text to the head or body section`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }

            pluginsConfigurations = mutableListOf(
                PluginConfigurationImpl(
                    DokkaBase::class.java.canonicalName,
                    DokkaConfiguration.SerializationFormat.JSON,
                    toJsonString(
                        DokkaBaseConfiguration(
                            customAssets = listOf(File("test/unknown-file.ext"))
                        )
                    )
                )
            )
        }

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class Test
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val testClassPage = writerPlugin.writer.contents
                    .getValue("root/test/-test/-test.html")
                    .let { Jsoup.parse(it) }

                val headChildNodes = testClassPage.head().childNodes()
                assertTrue("<head> section should not contain non-blank text nodes") {
                    headChildNodes.all { it !is TextNode || it.isBlank }
                }

                val bodyChildNodes = testClassPage.body().childNodes()
                assertTrue("<body> section should not contain non-blank text nodes. Something leaked from head?") {
                    bodyChildNodes.all { it !is TextNode || it.isBlank }
                }
            }
        }
    }

    @Test
    fun `should load script as defer if name ending in _deferred`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |class Test
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val generatedFiles = writerPlugin.writer.contents

                assertContains(generatedFiles.keys, "scripts/symbol-parameters-wrapper_deferred.js")

                val scripts = generatedFiles.getValue("root/test/-test/-test.html").let { Jsoup.parse(it) }.select("script")
                val deferredScriptSources = scripts.filter { element -> element.hasAttr("defer") }.map { it.attr("src") }

                // important to check symbol-parameters-wrapper_deferred specifically since it might break some features
                assertContains(deferredScriptSources, "../../../scripts/symbol-parameters-wrapper_deferred.js")
            }
        }
    }
}
