/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jsoup.Jsoup
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class JavadocAccessorNamingTest : AbstractJavadocTemplateMapTest() {

    val configuration = dokkaConfiguration {
        suppressObviousFunctions = true
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
                classpath = listOfNotNull(jvmStdlibPath)
            }
        }
    }

    /**
     * This is a quick sanity check for the AccessorMethodNamingTest
     */
    @Test
    fun verifySpecialIsRuleIsApplied() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            /src/main/kotlin/sample/TestCase.kt
            package sample
            
            /**
             * Test links:
             * - [TestCase.issuesFetched]
             * - [TestCase.isFoo]
             */
            data class TestCase(
               var issuesFetched: Int,
               var isFoo: String,
            )
            """.trimIndent(),
            configuration,
            cleanupOutput = false,
            pluginOverrides = listOf(writerPlugin, JavadocPlugin())
        ) {
            renderingStage = { _, _ ->
                val html = writerPlugin.writer.contents.getValue("sample/TestCase.html").let { Jsoup.parse(it) }
                val props = html
                    .select("#memberSummary_tabpanel")
                    .select("th[scope=row].colSecond")
                    .select("code")
                    .map { it.text() }
                    .toSet()
                // In K2 name of accessors parameter  is `value`
                assertTrue(
                    setOf(
                        "getIssuesFetched()",
                        "setIssuesFetched(Integer issuesFetched)",
                        "isFoo()",
                        "setFoo(String isFoo)",
                    ) == props || setOf(
                        "getIssuesFetched()",
                        "setIssuesFetched(Integer value)",
                        "isFoo()",
                        "setFoo(String value)",
                    ) == props
                )

                val descriptionLinks = html
                    .select("div.description")
                    .select("p")
                    .select("a")
                    .eachAttr("href")
                    .map { a -> a.takeLastWhile { it != '#' } }

                assertEquals(setOf(
                    "issuesFetched",
                    "isFoo()",
                ), descriptionLinks.toSet())

                // Make sure that the ids from above actually exist
                assertEquals(1, html.select("[id = isFoo()]").size)
                // Bug! Nothing in the doc has the right id
                assertEquals(0, html.select("[id = issuesFetched]").size)
            }
        }
    }
}
