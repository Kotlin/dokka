/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jsoup.Jsoup
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class JavadocThrowsTest : AbstractJavadocTemplateMapTest() {
    @Test
    fun `should render throws in method details section`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            /src/main/kotlin/sample/TestMethodThrows.kt
            package sample
            class TestMethodThrows {
                /**
                 * Method with throws tag
                 * @throws IllegalArgumentException if the argument is invalid
                 */
                fun doSomething(arg: Int) {}
            }
            """,
            configuration = configuration,
            cleanupOutput = false,
            pluginOverrides = listOf(writerPlugin, JavadocPlugin()),
        ) {
            renderingStage = { _, _ ->
                val html = writerPlugin.writer.contents.getValue("sample/TestMethodThrows.html").let { Jsoup.parse(it) }
                val throwsLabels = html.select(".details .throwsLabel")
                assertEquals(1, throwsLabels.size)
                assertEquals("Throws:", throwsLabels.first()!!.text())

                val ddElements = html.select(".details dl dd")
                val throwsDd = ddElements.first { it.text().contains("IllegalArgumentException") }
                assertTrue(throwsDd.html().contains("IllegalArgumentException"))
                assertTrue(throwsDd.text().contains("IllegalArgumentException - if the argument is invalid"))
            }
        }
    }

    @Test
    fun `should render throws in constructor details section`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            /src/main/kotlin/sample/TestConstructorThrows.kt
            package sample
            class TestConstructorThrows {
                /**
                 * @throws IllegalStateException when state is invalid
                 */
                constructor(flag: Boolean) {}
            }
            """,
            configuration = configuration,
            cleanupOutput = false,
            pluginOverrides = listOf(writerPlugin, JavadocPlugin()),
        ) {
            renderingStage = { _, _ ->
                val html = writerPlugin.writer.contents.getValue("sample/TestConstructorThrows.html").let { Jsoup.parse(it) }
                val throwsLabel = html.select(".details .throwsLabel").text()
                assertEquals("Throws:", throwsLabel)

                val dd = html.select(".details dl dd").first()
                assertTrue(dd != null && dd.text().contains("IllegalStateException - when state is invalid"))
            }
        }
    }
}
