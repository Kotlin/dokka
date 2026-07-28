/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import signatures.AbstractRenderingTest.brief
import signatures.AbstractRenderingTest.renderedSourceDependentContent
import utils.TestOutputWriterPlugin
import kotlin.collections.count
import kotlin.collections.forEach
import kotlin.collections.single
import kotlin.test.Test
import kotlin.test.assertEquals


class DivergentSignatureTest : signatures.AbstractRenderingTest() {

    @kotlin.test.Test
    fun `group { common + jvm + js }`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-clock/get-time.html")

                kotlin.test.assertEquals(3, content.count())
                val sourceSets = kotlin.collections.listOf("example/common", "example/js", "example/jvm")
                sourceSets.forEach {
                    kotlin.test.assertEquals("", content.select("[data-togglable=$it]").single().brief)
                }
            }
        }
    }

    @kotlin.test.Test
    fun `group { common + jvm }, group { js }`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content =
                    writerPlugin.renderedSourceDependentContent("example/example/-clock/get-times-in-millis.html")

                kotlin.test.assertEquals(3, content.count())
                kotlin.test.assertEquals(
                    "Time in minis",
                    content.select("[data-togglable=example/common]").single().brief
                )
                kotlin.test.assertEquals("Time in minis", content.select("[data-togglable=example/jvm]").single().brief)
                kotlin.test.assertEquals(
                    "JS implementation of getTimeInMillis",
                    content.select("[data-togglable=example/js]").single().brief
                )
            }
        }
    }

    @kotlin.test.Test
    fun `group { js }, group { jvm }, group { js }`() {

        val writerPlugin = utils.TestOutputWriterPlugin()

        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testFromData(
            signatures.AbstractRenderingTest.configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedSourceDependentContent("example/example/-clock/get-year.html")
                kotlin.test.assertEquals(3, content.count())
                kotlin.test.assertEquals(
                    "JVM custom kdoc",
                    content.select("[data-togglable=example/jvm]").single().brief
                )
                kotlin.test.assertEquals("JS custom kdoc", content.select("[data-togglable=example/js]").single().brief)
                kotlin.test.assertEquals("", content.select("[data-togglable=example/common]").single().brief)
            }
        }
    }
}
