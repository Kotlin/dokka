/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceSetFilterTest : BaseAbstractTest() {

    @Test // see #3011
    fun `should separate multiple data-filterable attribute values with comma`() {
        val configuration = dokkaConfiguration {
            moduleName = "Dokka Module"

            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/testing/Test.kt")
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/testing/Test.kt")
                }
            }
        }

        val source = """
            |/src/commonMain/kotlin/testing/Test.kt
            |package testing
            |
            |expect open class Test
            |
            |/src/jvmMain/kotlin/testing/Test.kt
            |package testing
            |
            |actual open class Test
        """.trimIndent()

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val packagePage = writerPlugin.writer.renderedContent("-dokka -module/testing/index.html")

                val testClassRow = packagePage
                    .select("div[data-togglable=TYPE]")
                    .select("div[class=table-row]")
                    .single()

                assertEquals("Dokka Module/common,Dokka Module/jvm", testClassRow.attr("data-filterable-current"))
                assertEquals("Dokka Module/common,Dokka Module/jvm", testClassRow.attr("data-filterable-set"))
            }
        }
    }
}
