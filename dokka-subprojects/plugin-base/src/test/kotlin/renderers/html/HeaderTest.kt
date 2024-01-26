/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jsoup.Jsoup
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
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
    fun `should include homepage link if homepageLink is provided`() {
        testRendering(
            DokkaBaseConfiguration(homepageLink = "https://github.com/Kotlin/dokka/")
        ) { _, _, writer ->
            val renderedContent = navigationElement(writer)

            val sourceLinkElement =
                assertNotNull(renderedContent.getElementById("homepage-link"), "Source link element not found")
            val aElement = assertNotNull(sourceLinkElement.selectFirst("a"))
            assertEquals("https://github.com/Kotlin/dokka/", aElement.attr("href"))
        }
    }

    @Test
    fun `should not include homepage link by default`() {
        testRendering(null) { _, _, writer ->
            val renderedContent = navigationElement(writer)
            assertNull(renderedContent.getElementById("homepage-link"), "Source link element found")
        }
    }

    private fun testRendering(
        baseConfiguration: DokkaBaseConfiguration?,
        block: (RootPageNode, DokkaContext, writer: TestOutputWriter) -> Unit
    ) {
        fun configuration(): DokkaConfigurationImpl {
            baseConfiguration ?: return configuration
            return configuration.copy(
                pluginsConfiguration = listOf(
                    PluginConfigurationImpl(
                        DokkaBase::class.java.canonicalName,
                        DokkaConfiguration.SerializationFormat.JSON,
                        toJsonString(baseConfiguration)
                    )
                )
            )
        }

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/jvm/Test.kt
            |fun test() {}
            |/src/js/Test.kt
            |fun test() {}
            """,
            configuration(),
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
