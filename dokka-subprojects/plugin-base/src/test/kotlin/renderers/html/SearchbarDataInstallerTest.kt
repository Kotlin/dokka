/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import utils.pagesJson
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchbarDataInstallerTest: BaseAbstractTest() {

    @Test // see #2289
    fun `should display description of root declarations without a leading dot`() {
        val configuration = dokkaConfiguration {
            moduleName = "Dokka Module"

            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/kotlin/Test.kt")
                }
            }
        }

        val source = """
            |/src/kotlin/Test.kt
            |
            |class Test
            |
        """.trimIndent()

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val searchRecords = writerPlugin.writer.pagesJson()

                assertEquals(
                    "Test",
                    searchRecords.find { record -> record.name == "class Test" }?.description ?: ""
                )
            }
        }
    }
}
