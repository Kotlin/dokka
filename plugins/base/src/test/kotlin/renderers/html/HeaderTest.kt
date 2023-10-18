/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import renderers.testPage
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeaderTest : HtmlRenderingOnlyTestBase() {
    override val renderedContent: Element
        get() = files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select(".navigation").single()

    @Test
    fun `should include link to github if sourceLinks are pointed to github`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://github.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        val context = context(
            configuration.copy(
                sourceSets = configuration.sourceSets.map {
                    it.copy(sourceLinks = setOf(sourceLink))
                }
            )
        )

        HtmlRenderer(context).render(testPage { })

        val sourceLinkElement =
            assertNotNull(renderedContent.getElementById("source-link"), "Source link element not found")
        val aElement = assertNotNull(sourceLinkElement.selectFirst("a"))
        assertEquals("https://github.com/Kotlin/dokka/", aElement.attr("href"))
    }

    @Test
    fun `should not include link to github if sourceLinks are different`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://github.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        val context = context(
            configuration.copy(
                sourceSets = listOf(
                    js.copy(sourceLinks = setOf(sourceLink)),
                    jvm.copy(sourceLinks = setOf(sourceLink.copy(remoteUrl = URL("https://github.com/Kotlin/dokkatoo/tree/main"))))
                )
            )
        )

        HtmlRenderer(context).render(testPage { })

        assertNull(renderedContent.getElementById("source-link"), "Source link element found")
    }

    @Test
    fun `should not include link to github if sourceLinks are pointed to gitlab`() {
        val sourceLink = SourceLinkDefinitionImpl(
            localDirectory = "",
            remoteUrl = URL("https://gitlab.com/Kotlin/dokka/tree/main"),
            remoteLineSuffix = null
        )
        val context = context(
            configuration.copy(
                sourceSets = configuration.sourceSets.map {
                    it.copy(sourceLinks = setOf(sourceLink))
                }
            )
        )

        HtmlRenderer(context).render(testPage { })

        assertNull(renderedContent.getElementById("source-link"), "Source link element found")
    }

    @Test
    fun `should not include link to github if there are no sourceLinks`() {
        val context = context(configuration)

        HtmlRenderer(context).render(testPage { })

        assertNull(renderedContent.getElementById("source-link"), "Source link element found")
    }
}
