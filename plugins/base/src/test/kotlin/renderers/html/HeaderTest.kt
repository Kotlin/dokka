/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jsoup.Jsoup
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeaderTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                name = "jvm"
                sourceRoots = listOf("src/jvm")
            }
            sourceSet {
                name = "js"
                sourceRoots = listOf("src/js")
            }
        }
    }

    @Test
    fun `should include link to github if sourceLinks are pointed to github`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://github.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        testRendering(
            configuration.copy(
                sourceSets = configuration.sourceSets.map {
                    it.copy(sourceLinks = setOf(sourceLink))
                }
            )
        ) { _, _, writer ->
            val renderedContent = navigationElement(writer)

            val sourceLinkElement =
                assertNotNull(renderedContent.getElementById("source-link"), "Source link element not found")
            val aElement = assertNotNull(sourceLinkElement.selectFirst("a"))
            assertEquals("https://github.com/Kotlin/dokka/", aElement.attr("href"))
        }
    }

    @Test
    fun `should not include link to github if sourceLinks are different`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://github.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        testRendering(
            configuration.copy(
                sourceSets = listOf(
                    configuration.sourceSets[0].copy(sourceLinks = setOf(sourceLink)),
                    configuration.sourceSets[1].copy(sourceLinks = setOf(sourceLink.copy(remoteUrl = URL("https://github.com/Kotlin/dokkatoo/tree/main"))))
                )
            )
        ) { _, _, writer ->
            val renderedContent = navigationElement(writer)
            assertNull(renderedContent.getElementById("source-link"), "Source link element found")
        }
    }

    @Test
    fun `should not include link to github if sourceLinks are pointed to gitlab`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://gitlab.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        testRendering(
            configuration.copy(
                sourceSets = configuration.sourceSets.map {
                    it.copy(sourceLinks = setOf(sourceLink))
                }
            )
        ) { _, _, writer ->
            val renderedContent = navigationElement(writer)
            assertNull(renderedContent.getElementById("source-link"), "Source link element found")
        }
    }

    @Test
    fun `should not include link to github if there are no sourceLinks`() {
        testRendering(configuration) { _, _, writer ->
            val renderedContent = navigationElement(writer)
            assertNull(renderedContent.getElementById("source-link"), "Source link element found")
        }
    }


    private fun testRendering(
        configuration: DokkaConfigurationImpl = this.configuration,
        block: (RootPageNode, DokkaContext, writer: TestOutputWriter) -> Unit
    ) {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/jvm/Test.kt
            |fun test() {}
            |/src/js/Test.kt
            |fun test() {}
            """,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { node, context ->
                block(node, context, writerPlugin.writer)
            }
        }
    }

    private fun navigationElement(writer: TestOutputWriter) =
        writer
            .contents
            .getValue("index.html")
            .let(Jsoup::parse)
            .select(".navigation")
            .single()

}
