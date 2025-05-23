/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jsoup.Jsoup
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

internal class JavadocConstructorsTest : AbstractJavadocTemplateMapTest() {
    @Test
    fun `should render full constructor description in constructor details section`() {
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
            /src/main/kotlin/sample/TestConstructor.kt
            package sample
            /** 
            * Documentation for TestConstructor 
            *
            * @constructor First line.
            *
            * Second line.
            *
            */
            class TestConstructor
            """,
            configuration = configuration,
            cleanupOutput = false,
            pluginOverrides = listOf(writerPlugin, JavadocPlugin()),
        ) {
            renderingStage = { _, _ ->
                val html = writerPlugin.writer.contents.getValue("sample/TestConstructor.html").let { Jsoup.parse(it) }
                val constructorDetailDescription = html
                    .select(".details")
                    .select("section:first-child")
                    .select("div.block")
                    .text()
                assertEquals("First line. Second line.", constructorDetailDescription)
            }
        }
    }
}
