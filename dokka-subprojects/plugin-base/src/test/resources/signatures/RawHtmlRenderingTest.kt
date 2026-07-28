/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jsoup.Jsoup
import signatures.AbstractRenderingTest.rawBrief
import signatures.AbstractRenderingTest.renderedContent
import signatures.AbstractRenderingTest.renderedSourceDependentContent
import utils.TestOutputWriterPlugin
import kotlin.collections.any
import kotlin.collections.count
import kotlin.collections.getValue
import kotlin.collections.single
import kotlin.let
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.contains

class RawHtmlRenderingTest: signatures.AbstractRenderingTest() {
    @kotlin.test.Test
    fun `work with raw html with inline comment`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-html-test/test.html")
                kotlin.test.assertEquals(1, content.count())
                kotlin.test.assertEquals(
                    content.select("[data-togglable=example/jvm]").single().rawBrief,
                    "This is an example <!-- not visible --> of html"
                )

                val indexContent = writerPlugin.writer.contents.getValue("example/example/-html-test/index.html")
                    .let { org.jsoup.Jsoup.parse(it) }
                kotlin.test.assertTrue(
                    indexContent.select("div.brief")
                        .any { it.html().contains("This is an example <!-- not visible --> of html") })
            }
        }
    }

    @kotlin.test.Test
    fun `work with raw html`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                //Module page
                val content = writerPlugin.renderedContent("example/example/index.html").select("div.brief")
                kotlin.test.assertTrue(content.size > 0)
                kotlin.test.assertTrue(content.any { it.html().contains("<!-- this shouldn't be visible -->") })
            }
        }
    }

    @kotlin.test.Test
    fun `work with raw, visible html`() {
        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-html-test/test-p.html")
                kotlin.test.assertEquals(1, content.count())
                kotlin.test.assertEquals(
                    content.select("[data-togglable=example/jvm]").single().rawBrief,
                    "This is an <b> documentation </b>"
                )

                val indexContent = writerPlugin.writer.contents.getValue("example/example/-html-test/index.html")
                    .let { org.jsoup.Jsoup.parse(it) }
                kotlin.test.assertTrue(
                    indexContent.select("div.brief").any { it.html().contains("This is an <b> documentation </b>") })
            }
        }
    }
}
