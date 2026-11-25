/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import signatures.renderedContent
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentTableTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `inline code in a KDoc table has only the language class`() = testCode(
        """
        |/src/main/kotlin/test/test.kt
        |package test
        |
        |/**
        | * | Method | Throws |
        | * |--------|--------|
        | * | `get(key)` | `NullPointerException` |
        | */
        |fun test() {}
        """.trimMargin()
    ) {
        val table = renderedContent("root/test/test.html").tableContainer()

        assertEquals(listOf("lang-kotlin", "lang-kotlin"), table.codeClasses())
        assertEquals(emptyList(), table.classesNamingStyleObjects())
    }

    @Test
    fun `inline code in a Java markdown comment table has only the language class`() = testCode(
        """
        |/src/main/java/example/A.java
        |package example;
        |
        |public class A {
        |    /// | Method | Throws |
        |    /// |--------|--------|
        |    /// | `get(Object key)` | `NullPointerException` |
        |    public void tables() {}
        |}
        """.trimMargin()
    ) {
        val table = renderedContent("root/example/-a/tables.html").tableContainer()

        assertEquals(listOf("lang-java", "lang-java"), table.codeClasses())
        assertEquals(emptyList(), table.classesNamingStyleObjects())
    }

    private fun Element.tableContainer(): Element = getElementsByClass("table--container").single()

    private fun Element.codeClasses(): List<String> = getElementsByTag("code").map { it.className() }

    /** Classes such as `org.jetbrains.dokka.pages.commenttable@1a2b3c`, made from a style's default `toString()`. */
    private fun Element.classesNamingStyleObjects(): List<String> =
        getAllElements().flatMap { it.classNames() }.filter { "@" in it || it.startsWith("org.jetbrains.") }

    private fun testCode(source: String, block: TestOutputWriter.() -> Unit) {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(source, configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _, _ ->
                writerPlugin.writer.block()
            }
        }
    }
}
