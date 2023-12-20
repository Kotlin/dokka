/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals


class DivergentSignatureTest : AbstractRenderingTest() {

    @Test
    fun `group { common + jvm + js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-clock/get-time.html")

                assertEquals(3, content.count())
                val sourceSets = listOf("example/common", "example/js", "example/jvm")
                sourceSets.forEach {
                    assertEquals("", content.select("[data-togglable=$it]").single().brief)
                }
            }
        }
    }

    @Test
    fun `group { common + jvm }, group { js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-clock/get-times-in-millis.html")

                assertEquals(3, content.count())
                assertEquals("Time in minis", content.select("[data-togglable=example/common]").single().brief)
                assertEquals("Time in minis", content.select("[data-togglable=example/jvm]").single().brief)
                assertEquals("JS implementation of getTimeInMillis", content.select("[data-togglable=example/js]").single().brief)
            }
        }
    }

    @Test
    fun `group { js }, group { jvm }, group { js }`() {

        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-clock/get-year.html")
                assertEquals(3, content.count())
                assertEquals("JVM custom kdoc", content.select("[data-togglable=example/jvm]").single().brief)
                assertEquals("JS custom kdoc", content.select("[data-togglable=example/js]").single().brief)
                assertEquals("", content.select("[data-togglable=example/common]").single().brief)
            }
        }
    }
}
