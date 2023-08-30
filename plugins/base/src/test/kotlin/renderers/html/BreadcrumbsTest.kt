/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import signatures.renderedContent
import utils.*
import kotlin.test.Test

class BreadcrumbsTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `should add breadcrumbs with current element`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/basic/TestClass.kt
            |package testpackage
            |
            |class TestClass {
            |    fun foo() {}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/testpackage/-test-class/foo.html").selectBreadcrumbs().match(
                    link("root"),
                    delimiter(),
                    link("testpackage"),
                    delimiter(),
                    link("TestClass"),
                    delimiter(),
                    current("foo"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @Test
    fun `should mark only one element as current even if more elements have the same name`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/basic/TestClass.kt
            |package testpackage
            |
            |class testname {
            |    val testname: String = ""
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/testpackage/testname/testname.html").selectBreadcrumbs().match(
                    link("root"),
                    delimiter(),
                    link("testpackage"),
                    delimiter(),
                    link("testname"),
                    delimiter(),
                    current("testname"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    private fun Element.selectBreadcrumbs() = this.select("div.breadcrumbs").single()

    private fun link(text: String): Tag = A(text)
    private fun delimiter(): Tag = Span().withClasses("delimiter")
    private fun current(text: String): Tag = Span(text).withClasses("current")
}
